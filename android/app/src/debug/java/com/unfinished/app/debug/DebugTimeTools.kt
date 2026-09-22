package com.unfinished.app.debug

/** Only packaged in debug builds; production never exposes time manipulation. */
object DebugTimeTools {
    private var offsetMillis: Long = 0

    fun now(realNow: Long = System.currentTimeMillis()): Long = realNow + offsetMillis

    fun advanceDays(days: Int) {
        offsetMillis += days.toLong() * 24L * 60L * 60L * 1000L
    }

    fun reset() {
        offsetMillis = 0
    }
}
