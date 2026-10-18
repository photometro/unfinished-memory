package com.unfinished.app.data.media

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** ADR-0011 v1 envelope. No Android dependency, so authentication tests run on the JVM. */
object MediaEnvelope {
    const val VERSION = 1
    const val NONCE_BYTES = 12
    const val TAG_BITS = 128
    const val HEADER_BYTES = 4 + 1 + 1 + 1 + NONCE_BYTES
    const val OVERHEAD_BYTES = HEADER_BYTES + TAG_BITS / 8
    private val magic = byteArrayOf('U'.code.toByte(), 'F'.code.toByte(), 'M'.code.toByte(), 'I'.code.toByte())

    enum class Role(val code: Int) { ORIGINAL(1), PREVIEW(2) }

    data class Binding(val vaultId: String, val memoryId: String, val assetId: String, val role: Role) {
        init { require(vaultId.isNotBlank() && memoryId.isNotBlank() && assetId.isNotBlank()) }
    }

    /** Big-endian u32 lengths + UTF-8 tuple fields; numeric fields use decimal ASCII. */
    fun aad(binding: Binding): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { data ->
            listOf("UFMI", VERSION.toString(), binding.role.code.toString(), binding.vaultId,
                binding.memoryId, binding.assetId).forEach { field ->
                val encoded = field.toByteArray(StandardCharsets.UTF_8)
                data.writeInt(encoded.size)
                data.write(encoded)
            }
        }
        bytes.toByteArray()
    }

    /** Caller owns streams and fsyncs before publishing a file. */
    fun encrypt(
        input: InputStream,
        output: OutputStream,
        key: SecretKey,
        binding: Binding,
        maxPlainBytes: Long,
        beforeChunk: (totalPlainBytes: Long, chunkBytes: Int) -> Unit = { _, _ -> }
    ): Long {
        require(maxPlainBytes > 0)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Keystore generates the randomized IV; never specify/reuse an encryption nonce.
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val nonce = cipher.iv
        require(nonce.size == NONCE_BYTES) { "Invalid media nonce" }
        cipher.updateAAD(aad(binding))
        output.write(magic)
        output.write(VERSION)
        output.write(binding.role.code)
        output.write(NONCE_BYTES)
        output.write(nonce)
        val chunk = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(chunk)
            if (count < 0) break
            if (count == 0) continue
            if (count.toLong() > maxPlainBytes - total) {
                throw MediaLimitException("照片超过安全大小上限", invalidInput = true)
            }
            total += count
            beforeChunk(total, count)
            cipher.update(chunk, 0, count)?.let(output::write)
        }
        if (total == 0L) throw MediaLimitException("照片文件为空", invalidInput = true)
        output.write(cipher.doFinal())
        return total
    }

    fun encrypt(plain: ByteArray, key: SecretKey, binding: Binding): ByteArray =
        ByteArrayOutputStream().use { output ->
            encrypt(ByteArrayInputStream(plain), output, key, binding, plain.size.toLong())
            output.toByteArray()
        }

    /** Nothing is returned until GCM authentication completes successfully. */
    fun decrypt(
        input: InputStream,
        key: SecretKey,
        binding: Binding,
        maxPlainBytes: Long,
        expectedPlainBytes: Long? = null
    ): ByteArray {
        if (expectedPlainBytes != null) {
            require(expectedPlainBytes in 1..maxPlainBytes && expectedPlainBytes <= Int.MAX_VALUE)
            val output = FixedPlainBuffer(expectedPlainBytes.toInt())
            decryptTo(input, output, key, binding, maxPlainBytes)
            return output.complete()
        }
        val output = ByteArrayOutputStream()
        decryptTo(input, output, key, binding, maxPlainBytes)
        return output.toByteArray()
    }

    fun decrypt(packed: ByteArray, key: SecretKey, binding: Binding, maxPlainBytes: Long): ByteArray {
        require(packed.size.toLong() <= maxPlainBytes + OVERHEAD_BYTES) { "媒体密文超过安全大小上限" }
        return decrypt(ByteArrayInputStream(packed), key, binding, maxPlainBytes,
            packed.size.toLong() - OVERHEAD_BYTES)
    }

    /** Verification only. Bytes written here must not be published before this returns. */
    fun decryptTo(input: InputStream, output: OutputStream, key: SecretKey, binding: Binding, maxPlainBytes: Long) {
        require(maxPlainBytes > 0)
        val data = DataInputStream(input)
        val headerMagic = ByteArray(magic.size).also(data::readFully)
        require(headerMagic.contentEquals(magic)) { "媒体文件格式错误" }
        require(data.readUnsignedByte() == VERSION) { "媒体文件版本不受支持" }
        require(data.readUnsignedByte() == binding.role.code) { "媒体文件类型不匹配" }
        require(data.readUnsignedByte() == NONCE_BYTES) { "媒体文件随机数长度无效" }
        val nonce = ByteArray(NONCE_BYTES).also(data::readFully)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad(binding))
        val chunk = ByteArray(16 * 1024)
        var encryptedBytes = 0L
        var plainBytes = 0L
        fun append(bytes: ByteArray?) {
            if (bytes == null) return
            require(bytes.size.toLong() <= maxPlainBytes - plainBytes) { "媒体内容超过安全大小上限" }
            plainBytes += bytes.size
            output.write(bytes)
        }
        while (true) {
            val count = input.read(chunk)
            if (count < 0) break
            if (count == 0) continue
            require(count.toLong() <= maxPlainBytes + TAG_BITS / 8 - encryptedBytes) {
                "媒体密文超过安全大小上限"
            }
            encryptedBytes += count
            append(cipher.update(chunk, 0, count))
        }
        require(encryptedBytes >= TAG_BITS / 8) { "媒体文件不完整" }
        append(cipher.doFinal())
        require(plainBytes > 0) { "媒体文件为空" }
    }

    fun cipherHash(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val chunk = ByteArray(16 * 1024)
        while (true) {
            val size = input.read(chunk)
            if (size < 0) break
            if (size > 0) digest.update(chunk, 0, size)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    /** Avoids a growing-buffer copy for a file whose authenticated envelope length is bounded. */
    private class FixedPlainBuffer(size: Int) : OutputStream() {
        private val bytes = ByteArray(size)
        private var written = 0
        override fun write(value: Int) {
            require(written < bytes.size) { "媒体内容长度无效" }
            bytes[written++] = value.toByte()
        }
        override fun write(value: ByteArray, offset: Int, length: Int) {
            require(length >= 0 && length <= bytes.size - written) { "媒体内容长度无效" }
            value.copyInto(bytes, written, offset, offset + length)
            written += length
        }
        fun complete(): ByteArray {
            require(written == bytes.size) { "媒体内容长度无效" }
            return bytes
        }
    }
}
