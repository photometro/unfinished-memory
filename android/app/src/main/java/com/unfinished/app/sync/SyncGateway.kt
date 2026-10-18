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
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val version: Long,
    val ciphertext: String,
    val contentHash: String,
    val deletedAt: Long? = null,
    val changeSequence: Long = 0L
)

data class PushAcknowledgement(val operationId: String, val outcome: String, val changeSequence: Long?)
data class PushResponse(val acknowledgements: List<PushAcknowledgement>, val cursor: Long)

interface SyncGateway {
    suspend fun push(records: List<EncryptedSyncRecord>): PushResponse
    suspend fun pull(cursor: Long, limit: Int = 200): PullBatch
}

data class PullBatch(val records: List<EncryptedSyncRecord>, val nextCursor: Long, val hasMore: Boolean)

/** HTTP adapter only accepts server-issued credentials; local offline IDs are never bearer tokens. */
class HttpSyncGateway(
    private val baseUrl: String,
    private val identityStore: VaultIdentityStore
) : SyncGateway {
    override suspend fun push(records: List<EncryptedSyncRecord>): PushResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().put("records", JSONArray().apply {
            records.forEach { record ->
                put(JSONObject().apply {
                    put("recordId", record.recordId)
                    put("operationId", record.operationId)
                    put("entityType", record.entityType)
                    put("entityId", record.entityId)
                    put("version", record.version)
                    put("ciphertext", record.ciphertext)
                    put("contentHash", record.contentHash)
                    record.deletedAt?.let { put("deletedAt", it) }
                })
            }
        })
        val json = JSONObject(request("/api/sync/push", "POST", body.toString()))
        val acks = mutableListOf<PushAcknowledgement>()
        val array = json.optJSONArray("acknowledgements") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            acks += PushAcknowledgement(
                item.getString("operationId"), item.getString("outcome"),
                if (item.isNull("changeSequence")) null else item.getLong("changeSequence")
            )
        }
        PushResponse(acks, json.optLong("cursor", 0L))
    }

    override suspend fun pull(cursor: Long, limit: Int): PullBatch = withContext(Dispatchers.IO) {
        val response = request("/api/sync/pull?cursor=$cursor&limit=$limit", "GET", null)
        val json = JSONObject(response)
        val items = mutableListOf<EncryptedSyncRecord>()
        val array = json.optJSONArray("records") ?: JSONArray()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            items += EncryptedSyncRecord(
                item.getString("recordId"), item.getString("operationId"), item.getString("entityType"),
                item.getString("entityId"), item.getLong("version"), item.getString("ciphertext"),
                item.getString("contentHash"), if (item.isNull("deletedAt")) null else item.getLong("deletedAt"),
                item.getLong("changeSequence")
            )
        }
        PullBatch(items, json.optLong("nextCursor", cursor), json.optBoolean("hasMore"))
    }

    private fun request(path: String, method: String, body: String?): String {
        val credentials = identityStore.requireRemoteCredentials()
        val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer ${credentials.deviceToken}")
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
