package com.unfinished.app.sync

import com.unfinished.app.data.VaultIdentityStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EncryptedSyncRecord(
    val recordId: String,
    val entityType: String,
    val entityId: String,
    val version: Long,
    val ciphertext: String,
    val contentHash: String,
    val deletedAt: Long? = null
)

interface SyncGateway {
    suspend fun push(records: List<EncryptedSyncRecord>): Long
    suspend fun pull(cursor: Long, limit: Int = 200): PullBatch
}

data class PullBatch(val records: List<EncryptedSyncRecord>, val nextCursor: Long, val hasMore: Boolean)

/** HTTP adapter for the anonymous server. It never sends decrypted Original/Cue content. */
class HttpSyncGateway(
    private val baseUrl: String,
    private val identityStore: VaultIdentityStore
) : SyncGateway {
    override suspend fun push(records: List<EncryptedSyncRecord>): Long = withContext(Dispatchers.IO) {
        val body = JSONObject().put("records", JSONArray().apply {
            records.forEach { record ->
                put(JSONObject().apply {
                    put("recordId", record.recordId)
                    put("entityType", record.entityType)
                    put("entityId", record.entityId)
                    put("version", record.version)
                    put("ciphertext", record.ciphertext)
                    put("contentHash", record.contentHash)
                    record.deletedAt?.let { put("deletedAt", it) }
                })
            }
        })
        val response = request("/api/sync/push", "POST", body.toString())
        JSONObject(response).optLong("cursor", 0L)
    }

    override suspend fun pull(cursor: Long, limit: Int): PullBatch = withContext(Dispatchers.IO) {
        val response = request("/api/sync/pull?cursor=$cursor&limit=$limit", "GET", null)
        val json = JSONObject(response)
        val items = mutableListOf<EncryptedSyncRecord>()
        val array = json.optJSONArray("records") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            items += EncryptedSyncRecord(
                item.getString("recordId"), item.getString("entityType"), item.getString("entityId"),
                item.getLong("version"), item.getString("ciphertext"), item.getString("contentHash"),
                if (item.isNull("deletedAt")) null else item.getLong("deletedAt")
            )
        }
        PullBatch(items, json.optLong("nextCursor", cursor), json.optBoolean("hasMore"))
    }

    private fun request(path: String, method: String, body: String?): String {
        val identity = identityStore.getOrCreate()
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer ${identity.deviceToken}")
            setRequestProperty("Content-Type", "application/json")
            doOutput = body != null
        }
        body?.let { connection.outputStream.use { stream -> stream.write(it.toByteArray(Charsets.UTF_8)) } }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) error("sync request failed: HTTP $code")
        return response
    }
}
