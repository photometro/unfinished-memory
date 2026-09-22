package com.unfinished.app.data

import android.content.Context
import java.security.MessageDigest

class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences("unfinished_lock", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean("enabled", false)

    fun setPin(pin: String) {
        require(pin.length >= 4) { "PIN must contain at least four digits" }
        prefs.edit().putString("pinHash", hash(pin)).putBoolean("enabled", true).apply()
    }

    fun disable() {
        prefs.edit().remove("pinHash").putBoolean("enabled", false).apply()
    }

    fun verify(pin: String): Boolean = prefs.getString("pinHash", null) == hash(pin)

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
