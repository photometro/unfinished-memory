package com.unfinished.app.sync

import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SyncPayloadContext(
    val vaultId: String,
    val entityType: String,
    val entityId: String,
    val entityVersion: Long
)

/** Client-side authenticated encryption for versioned UTF-8 text payloads. */
class SyncPayloadCodec(private val contentKey: ByteArray, private val keyId: String) {
    init {
        require(contentKey.size == 32) { "content key must be 256 bits" }
        require(keyId.isNotBlank())
    }

    fun encrypt(plaintext: String, context: SyncPayloadContext): String {
        require(context.vaultId.isNotBlank() && context.entityType.isNotBlank() && context.entityId.isNotBlank())
        require(context.entityVersion > 0)
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad(context))
        return JSONObject()
            .put("envelopeVersion", ENVELOPE_VERSION)
            .put("keyId", keyId)
            .put("algorithm", "A256GCM")
            .put("nonce", encode(nonce))
            .put("ciphertext", encode(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))))
            .toString()
    }

    fun decrypt(envelope: String, context: SyncPayloadContext): String {
        val json = JSONObject(envelope)
        require(json.getInt("envelopeVersion") == ENVELOPE_VERSION) { "unsupported sync envelope version" }
        require(json.getString("keyId") == keyId) { "content key id does not match" }
        require(json.getString("algorithm") == "A256GCM") { "unsupported sync encryption algorithm" }
        val nonce = decode(json.getString("nonce"))
        require(nonce.size == NONCE_BYTES)
        val ciphertext = decode(json.getString("ciphertext"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(contentKey, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad(context))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    companion object {
        private const val ENVELOPE_VERSION = 1
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128

        fun contentHash(envelope: String): String = MessageDigest.getInstance("SHA-256")
            .digest(envelope.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        private fun aad(context: SyncPayloadContext): ByteArray =
            "unfinished|sync|v1|${context.vaultId}|${context.entityType}|${context.entityId}|${context.entityVersion}"
                .toByteArray(Charsets.UTF_8)

        private fun encode(value: ByteArray) = android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)
        private fun decode(value: String) = android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
    }
}
