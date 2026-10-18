package com.unfinished.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

data class StoredMedia(
    val originalPath: String,
    val previewPath: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val originalBytes: Long,
    val cipherHash: String,
    val previewCipherHash: String
)

class MediaStoreException(message: String, cause: Throwable? = null, val invalidInput: Boolean = false) : IOException(message, cause)

/**
 * Private files only: a successful import is NOT a database commit. The gateway must serialize
 * import -> reference registration and reference snapshot -> recover with its outer vault lock.
 * Bitmaps returned by preview belong to the caller and must be released with its UI lifecycle.
 */
class LocalMediaStore(
    context: Context,
    private val vaultId: String,
    rootDirectory: File = File(context.noBackupFilesDir, "media/v1"),
    private val limits: MediaLimits = MediaLimits(),
    private val fault: (String) -> Unit = {},
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val availableHeapBytes: () -> Long = {
        Runtime.getRuntime().let { it.maxMemory() - (it.totalMemory() - it.freeMemory()) }
    }
) {
    private val resolver = context.applicationContext.contentResolver
    private val root = rootDirectory.canonicalFile
    private val mutex = Mutex()

    init { require(vaultId.isNotBlank() && keyAlias.isNotBlank()) }

    suspend fun importPhoto(
        memoryId: String,
        assetId: String,
        uri: Uri,
        existingOriginalBytes: Long
    ): StoredMedia = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureRoot()
            require(existingOriginalBytes >= 0) { "已有照片大小无效" }
            val binding = MediaEnvelope.Binding(vaultId, memoryId, assetId, MediaEnvelope.Role.ORIGINAL)
            val token = UUID.randomUUID().toString()
            val original = file("$token-original.ufmi")
            val preview = file("$token-preview.ufmi")
            val createdFiles = listOf(original, preview, part(original), part(preview))
            val taskContext = coroutineContext
            try {
                fault("before-copy")
                val knownBytes = declaredLength(uri)
                if (knownBytes > 0) limits.checkOriginalBytes(knownBytes, existingOriginalBytes)
                limits.checkSpace(root.usableSpace, knownBytes.coerceAtLeast(0L) + MediaEnvelope.OVERHEAD_BYTES,
                    knownBytes.coerceAtLeast(0L) + limits.maxPreviewBytes)
                val key = importKey()
                val copiedBytes = resolver.openInputStream(uri)?.use { source ->
                    FileOutputStream(part(original)).use { output ->
                        val count = MediaEnvelope.encrypt(source, output, key, binding, limits.maxOriginalBytes) { total, _ ->
                            taskContext.ensureActive()
                            fault("original-chunk")
                            limits.checkOriginalBytes(total, existingOriginalBytes)
                            val pendingCipherBytes = (total + MediaEnvelope.OVERHEAD_BYTES - part(original).length()).coerceAtLeast(0L)
                            limits.checkSpace(root.usableSpace, pendingCipherBytes, total + limits.maxPreviewBytes)
                        }
                        output.flush()
                        output.fd.sync()
                        count
                    }
                } ?: throw MediaStoreException("无法读取所选照片，请重新选择")
                fault("original-written")
                publish(original)
                fault("original-committed")
                taskContext.ensureActive()
                val decoded = readEncrypted(original, key, binding, limits.maxOriginalBytes).let { originalBytes ->
                    decode(originalBytes, originalInput = true)
                }
                val encodedPreview = try {
                    BoundedPreviewBuffer(limits.maxPreviewBytes).use { output ->
                        if (!decoded.bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, limits.previewQuality, output)) {
                            throw MediaStoreException("照片预览生成失败，请重试")
                        }
                        output.toByteArray()
                    }
                } finally {
                    decoded.bitmap.recycle()
                }
                taskContext.ensureActive()
                limits.checkSpace(root.usableSpace, encodedPreview.size.toLong() + MediaEnvelope.OVERHEAD_BYTES,
                    copiedBytes + encodedPreview.size)
                FileOutputStream(part(preview)).use { output ->
                    MediaEnvelope.encrypt(ByteArrayInputStream(encodedPreview), output, key,
                        binding.copy(role = MediaEnvelope.Role.PREVIEW), limits.maxPreviewBytes) { total, _ ->
                        taskContext.ensureActive()
                        fault("preview-chunk")
                        val pendingCipherBytes = (total + MediaEnvelope.OVERHEAD_BYTES - part(preview).length()).coerceAtLeast(0L)
                        limits.checkSpace(root.usableSpace, pendingCipherBytes, copiedBytes + encodedPreview.size)
                    }
                    output.flush()
                    output.fd.sync()
                }
                fault("preview-written")
                publish(preview)
                fault("preview-committed")
                verifyFile(original, key, binding, limits.maxOriginalBytes)
                verifyFile(preview, key, binding.copy(role = MediaEnvelope.Role.PREVIEW), limits.maxPreviewBytes)
                limits.checkSpace(root.usableSpace, 0, copiedBytes + encodedPreview.size)
                fault("verified")
                taskContext.ensureActive()
                StoredMedia(original.name, preview.name, decoded.mimeType, decoded.width, decoded.height,
                    copiedBytes, hash(original), hash(preview))
            } catch (error: Throwable) {
                // These are exclusively this import's fresh UUID paths, never existing references.
                createdFiles.forEach { candidate -> if (candidate.exists()) candidate.delete() }
                when (error) {
                    is CancellationException -> throw error
                    is MediaStoreException -> throw error
                    is MediaLimitException -> throw MediaStoreException(error.message ?: "照片不符合安全导入要求", error, error.invalidInput)
                    is ImageDecoder.DecodeException -> throw MediaStoreException("照片文件损坏或无法完整解码，请选择其他照片", error, invalidInput = true)
                    is OutOfMemoryError -> throw MediaStoreException("照片解码内存不足，原图未保存；请选择其他照片", error)
                    is SecurityException -> throw MediaStoreException("所选照片的访问权限已失效，请重新选择", error)
                    is IllegalArgumentException -> throw MediaStoreException(error.message ?: "照片不符合安全导入要求", error)
                    else -> throw MediaStoreException("照片导入失败，文字与其他照片已保留，请重试", error)
                }
            }
        }
    }

    suspend fun preview(memoryId: String, assetId: String, previewPath: String): Bitmap = withContext(Dispatchers.IO) {
        mutex.withLock {
            readOperation {
                val bytes = readEncrypted(file(previewPath), readKey(),
                    MediaEnvelope.Binding(vaultId, memoryId, assetId, MediaEnvelope.Role.PREVIEW), limits.maxPreviewBytes)
                decode(bytes, originalInput = false).bitmap
            }
        }
    }

    suspend fun verify(memoryId: String, assetId: String, originalPath: String, previewPath: String): Unit =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                readOperation {
                    val original = file(originalPath)
                    val readCeiling = compatibleOriginalReadCeiling(original)
                    val key = readKey()
                    val binding = MediaEnvelope.Binding(vaultId, memoryId, assetId, MediaEnvelope.Role.ORIGINAL)
                    verifyFile(original, key, binding, readCeiling)
                    verifyFile(file(previewPath), key, binding.copy(role = MediaEnvelope.Role.PREVIEW), limits.maxPreviewBytes)
                }
            }
        }

    /** Explicit bounded original read; never logs plaintext or returns it to a network service. */
    suspend fun originalBytes(memoryId: String, assetId: String, originalPath: String): ByteArray =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                readOperation {
                    val original = file(originalPath)
                    val readCeiling = compatibleOriginalReadCeiling(original)
                    readEncrypted(original, readKey(),
                        MediaEnvelope.Binding(vaultId, memoryId, assetId, MediaEnvelope.Role.ORIGINAL), readCeiling)
                }
            }
        }

    /** Caller holds the gateway's vault lock while obtaining this current reference set. */
    suspend fun recover(referencedPaths: Set<String>): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureRoot()
            val referenced = referencedPaths.map { file(it).name }.toSet()
            root.listFiles()?.forEach { candidate ->
                if (candidate.parentFile?.canonicalFile != root || !candidate.isFile) return@forEach
                val isPart = candidate.name.endsWith(".ufmi.part") &&
                    FILE_NAME.matches(candidate.name.removeSuffix(".part"))
                val isOrphan = FILE_NAME.matches(candidate.name) && candidate.name !in referenced
                if ((isPart || isOrphan) && !candidate.delete()) {
                    throw MediaStoreException("未完成媒体文件清理失败，请重试")
                }
            }
        }
    }

    /** Only for paths the caller has verified are no longer referenced by draft or Original. */
    suspend fun discard(paths: List<String>): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val checked = paths.map(::file)
            checked.forEach { candidate ->
                if (candidate.exists() && !candidate.delete()) throw MediaStoreException("照片移除未完成，请重试")
            }
        }
    }

    private fun ensureRoot() {
        if ((!root.exists() && !root.mkdirs()) || !root.isDirectory) {
            throw MediaStoreException("无法创建照片私有目录，请检查内部存储空间")
        }
    }

    private fun file(path: String): File {
        require(FILE_NAME.matches(path)) { "媒体文件标识无效" }
        return File(root, path).canonicalFile.also {
            require(it.parentFile == root && it.name == path) { "媒体文件标识超出私有目录" }
        }
    }

    private fun part(target: File) = File(root, "${target.name}.part")

    private fun publish(target: File) {
        require(!target.exists()) { "媒体文件标识已存在" }
        Files.move(part(target).toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        // fsync the directory entry, not only the contents of the pre-rename .part file.
        val descriptor = Os.open(root.path, OsConstants.O_RDONLY, 0)
        try { Os.fsync(descriptor) } finally { Os.close(descriptor) }
    }

    private fun declaredLength(uri: Uri): Long = runCatching {
        resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    }.getOrDefault(-1L)

    private data class Decoded(val bitmap: Bitmap, val mimeType: String, val width: Int, val height: Int)

    private fun decode(bytes: ByteArray, originalInput: Boolean): Decoded {
        // Some platform decoders expose APNG as a static PNG. Do not silently retain only frame 1.
        if (isAnimatedPng(bytes)) throw MediaStoreException("暂不支持动画图片，请选择静态照片", invalidInput = true)
        var mime = ""
        var width = 0
        var height = 0
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            if (info.isAnimated) throw MediaStoreException("暂不支持动画图片，请选择静态照片", invalidInput = true)
            mime = info.mimeType
            val allowed = mime in BASE_MIME_TYPES || (mime in OPTIONAL_MIME_TYPES && ImageDecoder.isMimeTypeSupported(mime))
            if (!allowed) throw MediaStoreException("暂不支持这种照片格式，请选择 JPEG、PNG 或静态 WebP", invalidInput = true)
            width = info.size.width
            height = info.size.height
            limits.checkDimensions(width, height)
            if (!originalInput) {
                require(width <= limits.previewLongEdge && height <= limits.previewLongEdge) { "照片预览尺寸无效" }
            }
            val target = limits.previewSize(width, height)
            decoder.setTargetSize(target.first, target.second)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            // The preview is a bounded sRGB display derivative; the private original stays byte-exact.
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            decoder.setOnPartialImageListener { false }
        }
        if (bitmap.width > limits.previewLongEdge || bitmap.height > limits.previewLongEdge) {
            bitmap.recycle()
            throw MediaStoreException("照片预览尺寸超过安全上限")
        }
        return Decoded(bitmap, mime, width, height)
    }

    private fun isAnimatedPng(bytes: ByteArray): Boolean {
        val signature = byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10)
        if (bytes.size < 8 || !bytes.copyOfRange(0, 8).contentEquals(signature)) return false
        var offset = 8
        while (offset.toLong() + 12L <= bytes.size) {
            val size = ByteBuffer.wrap(bytes, offset, 4).int.toLong()
            if (size < 0 || offset.toLong() + 12L + size > bytes.size) return false
            if (bytes[offset + 4] == 97.toByte() && bytes[offset + 5] == 99.toByte() &&
                bytes[offset + 6] == 84.toByte() && bytes[offset + 7] == 76.toByte()) return true
            offset = (offset.toLong() + 12L + size).toInt()
        }
        return false
    }

    private fun compatibleOriginalReadCeiling(file: File): Long {
        val ceiling = limits.storedOriginalReadCeiling
        require(file.isFile && file.length() in MediaEnvelope.OVERHEAD_BYTES.toLong()..(ceiling + MediaEnvelope.OVERHEAD_BYTES)) {
            "照片文件缺失、不完整或超过格式读取上限"
        }
        limits.checkStoredOriginalReadMemory(file.length() - MediaEnvelope.OVERHEAD_BYTES, availableHeapBytes())
        return ceiling
    }

    private fun readEncrypted(file: File, key: SecretKey, binding: MediaEnvelope.Binding, max: Long): ByteArray {
        require(file.isFile && file.length() in MediaEnvelope.OVERHEAD_BYTES.toLong()..(max + MediaEnvelope.OVERHEAD_BYTES)) {
            "照片文件缺失、不完整或超过安全上限"
        }
        return file.inputStream().buffered().use {
            MediaEnvelope.decrypt(it, key, binding, max, file.length() - MediaEnvelope.OVERHEAD_BYTES)
        }
    }

    private fun verifyFile(file: File, key: SecretKey, binding: MediaEnvelope.Binding, max: Long) {
        require(file.isFile && file.length() in MediaEnvelope.OVERHEAD_BYTES.toLong()..(max + MediaEnvelope.OVERHEAD_BYTES)) {
            "照片文件缺失、不完整或超过安全上限"
        }
        file.inputStream().buffered().use { input -> MediaEnvelope.decryptTo(input, DISCARD, key, binding, max) }
    }

    private fun hash(file: File) = file.inputStream().buffered().use(MediaEnvelope::cipherHash)

    private fun importKey(): SecretKey = synchronized(KEY_LOCK) {
        val store = keyStore()
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return@synchronized it }
        if (store.containsAlias(keyAlias) || root.listFiles()?.any { FILE_NAME.matches(it.name) } == true) {
            throw MediaStoreException("照片加密密钥缺失，已有照片未改动；不能自动重建密钥")
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
            .build())
        generator.generateKey()
    }

    private fun readKey(): SecretKey = keyStore().getKey(keyAlias, null) as? SecretKey
        ?: throw MediaStoreException("照片加密密钥缺失，照片未改动；无法读取")

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private inline fun <T> readOperation(block: () -> T): T = try {
        block()
    } catch (error: Throwable) {
        when (error) {
            is CancellationException -> throw error
            is MediaStoreException -> throw error
            is AEADBadTagException -> throw MediaStoreException("照片认证失败，文件未改动；请勿重新生成密钥", error)
            is OutOfMemoryError -> throw MediaStoreException("当前内存不足以读取或校验原图，文件和预览已保留；请稍后重试。", error)
            is IllegalArgumentException -> throw MediaStoreException(error.message ?: "照片文件不符合读取要求", error)
            else -> throw MediaStoreException("照片读取失败，文件未改动", error)
        }
    }

    private class BoundedPreviewBuffer(private val maximum: Long) : ByteArrayOutputStream() {
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            require(length >= 0 && length.toLong() <= maximum - size()) { "照片预览编码超过安全上限" }
            super.write(bytes, offset, length)
        }
        override fun write(value: Int) {
            require(size().toLong() < maximum) { "照片预览编码超过安全上限" }
            super.write(value)
        }
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "unfinished.media.aes.v1"
        private val KEY_LOCK = Any()
        private val FILE_NAME = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-(original|preview)\\.ufmi")
        private val BASE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private val OPTIONAL_MIME_TYPES = setOf("image/heif", "image/heic", "image/avif")
        private val DISCARD = object : OutputStream() {
            override fun write(value: Int) = Unit
            override fun write(bytes: ByteArray, offset: Int, length: Int) = Unit
        }
    }
}
