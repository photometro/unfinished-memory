package com.unfinished.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Device-local protection and a versioned recovery envelope for a random content key.
 * Human-readable recovery UX and the legacy 16-digit code are deliberately not connected here.
 */
class ContentKeyEnvelope(context: Context, preferencesName: String = "unfinished_sync_keys") {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun getOrCreateLocalContentKey(): ByteArray {
        val persisted = preferences.getString(LOCAL_KEY_PREF, null)
        if (persisted != null) return unwrapLocal(persisted)

        val contentKey = ByteArray(KEY_BYTES).also(random::nextBytes)
        val wrapped = wrapLocal(contentKey)
        check(preferences.edit().putString(LOCAL_KEY_PREF, wrapped).commit()) {
            "failed to persist the wrapped content key"
        }
        return contentKey
    }

    fun createRecoveryEnvelope(contentKey: ByteArray, recoverySecret: ByteArray, vaultId: String, keyId: String): String =
        ContentKeyEnvelopeCrypto.create(contentKey, recoverySecret, vaultId, keyId)

    fun openRecoveryEnvelope(envelope: String, recoverySecret: ByteArray, expectedVaultId: String): ByteArray =
        ContentKeyEnvelopeCrypto.open(envelope, recoverySecret, expectedVaultId)

    private fun wrapLocal(contentKey: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deviceKey())
        val iv = cipher.iv
        return Base64.encodeToString(iv + cipher.doFinal(contentKey), Base64.NO_WRAP)
    }

    private fun unwrapLocal(encoded: String): ByteArray {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > IV_BYTES) { "invalid local content-key envelope" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deviceKey(), GCMParameterSpec(TAG_BITS, payload.copyOfRange(0, IV_BYTES)))
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size)).also {
            require(it.size == KEY_BYTES) { "invalid local content key" }
        }
    }

    private fun deviceKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(DEVICE_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(DEVICE_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val DEVICE_KEY_ALIAS = "unfinished.sync.content-key-wrap.v1"
        private const val LOCAL_KEY_PREF = "local-content-key-v1"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val KEY_BYTES = 32
    }
}

/** Pure crypto helpers are kept separate so JVM tests can exercise tamper/wrong-key failures. */
object ContentKeyEnvelopeCrypto {
    private const val ENVELOPE_VERSION = 1
    private const val KEY_BYTES = 32
    private const val IV_BYTES = 12
    private const val SALT_BYTES = 16
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun create(contentKey: ByteArray, recoverySecret: ByteArray, vaultId: String, keyId: String): String {
        require(contentKey.size == KEY_BYTES) { "content key must be 256 bits" }
        require(recoverySecret.size == KEY_BYTES) { "recovery secret must be 256 bits" }
        require(vaultId.isNotBlank() && keyId.isNotBlank())
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val derived = hkdfSha256(recoverySecret, salt, info(vaultId, keyId), KEY_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(vaultId, keyId))
        val ciphertext = cipher.doFinal(contentKey)
        val vaultBytes = vaultId.toByteArray(Charsets.UTF_8)
        val keyIdBytes = keyId.toByteArray(Charsets.UTF_8)
        require(vaultBytes.size <= 65535 && keyIdBytes.size in 1..65535)
        val packed = ByteBuffer.allocate(1 + 2 + 2 + SALT_BYTES + IV_BYTES + ciphertext.size + vaultBytes.size + keyIdBytes.size)
            .put(ENVELOPE_VERSION.toByte())
            .putShort(vaultBytes.size.toShort())
            .putShort(keyIdBytes.size.toShort())
            .put(salt)
            .put(iv)
            .put(ciphertext)
            .put(vaultBytes)
            .put(keyIdBytes)
            .array()
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(packed)
    }

    fun open(envelope: String, recoverySecret: ByteArray, expectedVaultId: String): ByteArray {
        require(recoverySecret.size == KEY_BYTES) { "recovery secret must be 256 bits" }
        val packed = java.util.Base64.getUrlDecoder().decode(envelope)
        require(packed.size >= 1 + 2 + 2 + SALT_BYTES + IV_BYTES + KEY_BYTES + TAG_BITS / 8)
        val buffer = ByteBuffer.wrap(packed)
        require(buffer.get().toInt() == ENVELOPE_VERSION)
        val vaultLength = buffer.short.toInt() and 0xffff
        val keyIdLength = buffer.short.toInt() and 0xffff
        require(vaultLength > 0 && keyIdLength > 0)
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val iv = ByteArray(IV_BYTES).also(buffer::get)
        val ciphertext = ByteArray(KEY_BYTES + TAG_BITS / 8).also(buffer::get)
        require(vaultLength + keyIdLength == buffer.remaining()) { "invalid recovery envelope length" }
        val vaultId = ByteArray(vaultLength).also(buffer::get).toString(Charsets.UTF_8)
        val keyId = ByteArray(keyIdLength).also(buffer::get).toString(Charsets.UTF_8)
        require(vaultId == expectedVaultId) { "recovery envelope belongs to another vault" }
        val derived = hkdfSha256(recoverySecret, salt, info(vaultId, keyId), KEY_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(vaultId, keyId))
        return cipher.doFinal(ciphertext).also { require(it.size == KEY_BYTES) }
    }

    private fun info(vaultId: String, keyId: String) = "unfinished/recovery/v1/$vaultId/$keyId".toByteArray(Charsets.UTF_8)
    private fun aad(vaultId: String, keyId: String) = "unfinished|recovery|v1|$vaultId|$keyId".toByteArray(Charsets.UTF_8)

    private fun hkdfSha256(inputKey: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val pseudoRandomKey = mac.doFinal(inputKey)
        val output = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            mac.init(SecretKeySpec(pseudoRandomKey, "HmacSHA256"))
            mac.update(previous)
            mac.update(info)
            mac.update(counter.toByte())
            previous = mac.doFinal()
            val take = minOf(previous.size, length - offset)
            previous.copyInto(output, offset, 0, take)
            offset += take
            counter++
        }
        return output
    }
}
