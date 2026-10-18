package com.unfinished.app.data.media

import kotlin.math.max
import kotlin.math.roundToInt

class MediaLimitException(message: String, val invalidInput: Boolean) : IllegalArgumentException(message)

/** Engineering guardrails from ADR-0011; values are not a universal format guarantee. */
data class MediaLimits(
    // Keystore GCM buffers plaintext until authentication. A 64 MiB original
    // exceeds the measured 192 MiB heap; input chunks alone do not bound it.
    val maxOriginalBytes: Long = 32L * MIB,
    val maxMemoryOriginalBytes: Long = 512L * MIB,
    val maxInputPixels: Long = 200_000_000L,
    val previewLongEdge: Int = 1024,
    val previewQuality: Int = 90,
    val minFreeReserveBytes: Long = 64L * MIB
) {
    init {
        require(maxOriginalBytes in 1..Int.MAX_VALUE.toLong())
        require(maxMemoryOriginalBytes >= maxOriginalBytes)
        require(maxInputPixels > 0)
        require(previewLongEdge in 1..4096)
        require(previewQuality in 0..100)
        require(minFreeReserveBytes >= 0)
    }

    fun checkOriginalBytes(bytes: Long, existingOriginalBytes: Long) {
        require(existingOriginalBytes >= 0) { "已有照片大小无效" }
        if (bytes !in 1..maxOriginalBytes) {
            throw MediaLimitException("照片为空或超过单张安全大小，请选择其他照片", invalidInput = true)
        }
        if (existingOriginalBytes > maxMemoryOriginalBytes || bytes > maxMemoryOriginalBytes - existingOriginalBytes) {
            throw MediaLimitException("这一段记忆的照片总大小超过安全上限，请移除部分照片后重试", invalidInput = false)
        }
    }

    fun checkDimensions(width: Int, height: Int) {
        if (width <= 0 || height <= 0 || width.toLong() * height.toLong() > maxInputPixels) {
            throw MediaLimitException("照片尺寸超过安全解码上限或尺寸无效，请选择其他照片", invalidInput = true)
        }
    }

    fun previewSize(width: Int, height: Int): Pair<Int, Int> {
        checkDimensions(width, height)
        val longest = max(width, height)
        if (longest <= previewLongEdge) return width to height
        val ratio = previewLongEdge.toDouble() / longest
        return max(1, (width * ratio).roundToInt()) to max(1, (height * ratio).roundToInt())
    }

    val maxPreviewBytes: Long get() = previewLongEdge.toLong() * previewLongEdge * 4L + 64L * 1024L

    // Import calibration is not a format migration. Keep earlier v1 files
    // recognizable even when a new import ceiling becomes more conservative.
    val storedOriginalReadCeiling: Long get() = max(64L * MIB, maxOriginalBytes)

    fun checkStoredOriginalReadMemory(bytes: Long, availableHeap: Long) {
        if (bytes > maxOriginalBytes) {
            // Historical full-file GCM reads can retain/grow provider buffers
            // and the caller result at once. This is a resource gate, not an
            // invalid-content classification. Preview remains independent.
            val required = bytes * 6L + 16L * MIB
            if (availableHeap < required) throw MediaLimitException(
                "当前内存不足以读取或校验原图，文件和预览已保留；请稍后重试。", invalidInput = false)
        }
    }

    fun reserveBytes(totalNewBytes: Long): Long {
        require(totalNewBytes >= 0)
        return max(minFreeReserveBytes, totalNewBytes / 10L + if (totalNewBytes % 10L == 0L) 0L else 1L)
    }

    fun checkSpace(availableBytes: Long, bytesStillToWrite: Long, totalNewBytes: Long) {
        val reserve = reserveBytes(totalNewBytes)
        if (availableBytes < 0 || bytesStillToWrite < 0 || availableBytes < reserve || bytesStillToWrite > availableBytes - reserve) {
            throw MediaLimitException("设备内部存储空间不足，照片未保存；请释放空间后重试", invalidInput = false)
        }
    }

    companion object { const val MIB = 1024L * 1024L }
}
