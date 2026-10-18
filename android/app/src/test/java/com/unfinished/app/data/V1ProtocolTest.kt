package com.unfinished.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class V1ProtocolTest {
    private val now = Instant.parse("2026-10-06T03:00:00Z").toEpochMilli()
    private fun memory(days: Long = 14) = MemoryEntity("m", "legacy body prefix", "cipher", now, "SEALED", "SERENDIPITY", now - days * V1Rules.DAY_MS, null, 0, now, now)
    private val policy = ResurfacePolicyEntity("m", "SERENDIPITY", 14, null, false)

    @Test fun dormancyAndPoolsNeverUseSevenDayException() {
        assertFalse(V1Rules.eligible(memory(13), policy, now))
        assertTrue(V1Rules.eligible(memory(14), policy, now))
        assertFalse(V1Rules.eligible(memory(30), policy.copy(mode = "QUIET"), now))
        assertFalse(V1Rules.eligible(memory(30).copy(cooldownUntil = now + 1), policy, now))
        assertFalse(V1Rules.eligible(memory(30).copy(deferUntil = now + 1), policy, now))
        assertFalse(V1Rules.eligible(memory(30), policy.copy(mode = "FIXED_DATE"), now))
    }
    @Test fun localDateNotZoneOrTwentyFourHourBuckets() {
        val instant = Instant.ofEpochMilli(now)
        assertEquals(V1Rules.dateKey(instant, ZoneId.of("Asia/Shanghai")), V1Rules.dateKey(instant, ZoneId.of("Asia/Tokyo")))
        assertEquals("2026-11-01", V1Rules.dateKey(Instant.parse("2026-11-01T05:30:00Z"), ZoneId.of("America/New_York")))
        assertEquals("2026-11-01", V1Rules.dateKey(Instant.parse("2026-11-01T06:30:00Z"), ZoneId.of("America/New_York")))
        assertEquals("2026-10-07", V1Rules.dateKey(Instant.parse("2026-10-06T16:00:00Z"), ZoneId.of("Asia/Shanghai")))
    }
    @Test fun legacyNeverBecomesBlindAndLabelNeverUsesBody() {
        assertEquals("FIRST_BLIND_RECALL", V1Rules.recallMode(memory()))
        assertEquals("LEGACY_UNKNOWN", V1Rules.recallMode(memory().copy(exposureHistory = "LEGACY_UNKNOWN")))
        assertEquals("POST_EXPOSURE_RECALL", V1Rules.recallMode(memory().copy(lastOriginalExposureAt = now)))
        assertFalse(V1Rules.displayLabel(memory()).contains("legacy"))
        assertEquals("user title", V1Rules.displayLabel(memory().copy(userTitle = "user title")))
    }
}
