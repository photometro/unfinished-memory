package com.unfinished.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class VaultBootstrapClient(private val baseUrl: String) {
    suspend fun bootstrap(): VaultIdentity = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl.trimEnd('/') + "/api/device/bootstrap").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 20_000
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) error("bootstrap failed: HTTP $code")
        val json = JSONObject(body)
        VaultIdentity(json.getString("vaultId"), json.getString("deviceId"), json.getString("deviceToken"))
    }
}
