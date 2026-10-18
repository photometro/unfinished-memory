package com.unfinished.app.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MediaLimitsTest {
    private val limits = MediaLimits()

    @Test fun acceptsBoundaryBytesButRejectsEmptySingleAndCumulativeOverflow() {
        limits.checkOriginalBytes(32L * MediaLimits.MIB, 480L * MediaLimits.MIB)
        assertThrows(IllegalArgumentException::class.java) { limits.checkOriginalBytes(0, 0) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkOriginalBytes(32L * MediaLimits.MIB + 1, 0) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkOriginalBytes(1, 512L * MediaLimits.MIB) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkOriginalBytes(1, Long.MAX_VALUE) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkOriginalBytes(1, -1) }
    }

    @Test fun pixelCountUsesLongAndPreviewIsBoundedWithoutUpscaling() {
        limits.checkDimensions(20_000, 10_000)
        assertThrows(IllegalArgumentException::class.java) { limits.checkDimensions(20_001, 10_000) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkDimensions(Int.MAX_VALUE, Int.MAX_VALUE) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkDimensions(0, 200) }
        assertEquals(1024 to 768, limits.previewSize(4000, 3000))
        assertEquals(1 to 1024, limits.previewSize(1, 20_000))
        assertEquals(32 to 48, limits.previewSize(32, 48))
    }

    @Test fun storageRequiresRemainingDataAndReserveUsingOverflowSafeArithmetic() {
        val minimum = 64L * MediaLimits.MIB
        limits.checkSpace(minimum + 100, 100, 1000)
        assertThrows(IllegalArgumentException::class.java) { limits.checkSpace(minimum + 99, 100, 1000) }
        assertThrows(IllegalArgumentException::class.java) { limits.checkSpace(1000, Long.MAX_VALUE, Long.MAX_VALUE) }
        assertEquals(100_000_001L, limits.reserveBytes(1_000_000_001L))
        assertEquals(minimum, limits.reserveBytes(1))
    }

    @Test fun inputLimitAndTemporarySpaceOrTotalLimitHaveDifferentFailureKinds() {
        val input = assertThrows(MediaLimitException::class.java) { limits.checkOriginalBytes(0, 0) }
        val total = assertThrows(MediaLimitException::class.java) { limits.checkOriginalBytes(1, limits.maxMemoryOriginalBytes) }
        val space = assertThrows(MediaLimitException::class.java) { limits.checkSpace(0, 1, 1) }
        assertEquals(true, input.invalidInput)
        assertEquals(false, total.invalidInput)
        assertEquals(false, space.invalidInput)
    }

    @Test fun historicalReadCeilingAndInsufficientHeapAreNotNewImportValidity() {
        assertEquals(32L * MediaLimits.MIB, limits.maxOriginalBytes)
        assertEquals(64L * MediaLimits.MIB, limits.storedOriginalReadCeiling)
        val legacyBytes = 64L * MediaLimits.MIB
        val unavailable = assertThrows(MediaLimitException::class.java) {
            limits.checkStoredOriginalReadMemory(legacyBytes, 192L * MediaLimits.MIB)
        }
        assertEquals(false, unavailable.invalidInput)
        limits.checkStoredOriginalReadMemory(legacyBytes, 400L * MediaLimits.MIB)
    }
}
