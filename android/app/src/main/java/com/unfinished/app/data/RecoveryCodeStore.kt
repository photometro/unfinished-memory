package com.unfinished.app.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/** V1 keeps recovery local; server-side account binding remains a future gateway. */
class RecoveryCodeStore(context: Context) {
    private val prefs = context.getSharedPreferences("unfinished_recovery", Context.MODE_PRIVATE)
    private val random = SecureRandom()

    fun createOrGet(): String {
        val existing = prefs.getString("code", null)
        if (existing != null) return existing
        val code = buildString { repeat(4) { append((1000 + random.nextInt(9000)).toString()); if (it < 3) append('-') } }
        prefs.edit().putString("code", code).putString("hash", hash(code)).apply()
        return code
    }

    fun verify(code: String): Boolean = prefs.getString("hash", null) == hash(code)

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
