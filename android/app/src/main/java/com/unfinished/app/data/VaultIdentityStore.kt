package com.unfinished.app.data

import android.content.Context
import java.util.UUID

data class VaultIdentity(
    val vaultId: String,
    val deviceId: String,
    val deviceToken: String
)

class VaultIdentityStore(context: Context) {
    private val prefs = context.getSharedPreferences("unfinished_vault", Context.MODE_PRIVATE)

    fun getOrCreate(): VaultIdentity {
        val vaultId = prefs.getString("vaultId", null) ?: UUID.randomUUID().toString()
        val deviceId = prefs.getString("deviceId", null) ?: UUID.randomUUID().toString()
        val token = prefs.getString("deviceToken", null) ?: UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString("vaultId", vaultId).putString("deviceId", deviceId).putString("deviceToken", token).apply()
        return VaultIdentity(vaultId, deviceId, token)
    }
}
