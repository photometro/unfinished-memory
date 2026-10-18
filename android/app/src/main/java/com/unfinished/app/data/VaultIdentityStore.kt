package com.unfinished.app.data

import android.content.Context
import java.util.UUID

/** Stable offline namespace used by #9; this identity is never a server credential. */
data class LocalVaultIdentity(val vaultId: String, val deviceId: String)

/** Server-issued identity returned only after a successful anonymous bootstrap. */
data class VaultIdentity(val vaultId: String, val deviceId: String, val deviceToken: String)

class VaultIdentityStore(
    context: Context,
    private val cipher: LocalCipher = LocalCipher(),
    preferencesName: String = "unfinished_vault"
) {
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun getOrCreate(): LocalVaultIdentity {
        val vaultId = prefs.getString(LOCAL_VAULT_ID, null) ?: UUID.randomUUID().toString()
        val deviceId = prefs.getString(LOCAL_DEVICE_ID, null) ?: UUID.randomUUID().toString()
        check(prefs.edit().putString(LOCAL_VAULT_ID, vaultId).putString(LOCAL_DEVICE_ID, deviceId).commit()) {
            "failed to persist local vault namespace"
        }
        return LocalVaultIdentity(vaultId, deviceId)
    }

    fun remoteCredentialsOrNull(): VaultIdentity? {
        val vaultId = prefs.getString(REMOTE_VAULT_ID, null) ?: return null
        val deviceId = prefs.getString(REMOTE_DEVICE_ID, null) ?: return null
        val encryptedToken = prefs.getString(REMOTE_TOKEN, null) ?: return null
        val token = cipher.decrypt(encryptedToken)
        require(token.isNotBlank()) { "stored remote device token is empty" }
        return VaultIdentity(vaultId, deviceId, token)
    }

    fun requireRemoteCredentials(): VaultIdentity = remoteCredentialsOrNull()
        ?: error("remote vault bootstrap has not completed; local-only use remains available")

    fun saveRemoteCredentials(identity: VaultIdentity) {
        require(identity.vaultId.isNotBlank() && identity.deviceId.isNotBlank() && identity.deviceToken.isNotBlank())
        // The server scope is intentionally stored separately from the existing local #9 scope.
        val encryptedToken = cipher.encrypt(identity.deviceToken)
        check(
            prefs.edit()
                .putString(REMOTE_VAULT_ID, identity.vaultId)
                .putString(REMOTE_DEVICE_ID, identity.deviceId)
                .putString(REMOTE_TOKEN, encryptedToken)
                .commit()
        ) { "failed to persist remote vault credentials" }
    }

    private companion object {
        const val LOCAL_VAULT_ID = "vaultId" // Preserve the established #9 local scope.
        const val LOCAL_DEVICE_ID = "deviceId"
        const val REMOTE_VAULT_ID = "remoteVaultId"
        const val REMOTE_DEVICE_ID = "remoteDeviceId"
        const val REMOTE_TOKEN = "encryptedRemoteDeviceToken"
    }
}
