package com.rtw.pro.notification.data

import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * POSTs a topic notification to the backend (e.g. Firebase Cloud Function).
 * Server must verify [Authorization: Bearer &lt;Firebase ID token&gt;] and call FCM Admin send.
 */
fun interface TopicPushHttpExecutor {
    fun post(url: String, headers: Map<String, String>, jsonBody: String): TopicPushHttpResult
}

data class TopicPushHttpResult(
    val statusCode: Int,
    val body: String
)

class AndroidTopicPushHttpExecutor(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 20_000
) : TopicPushHttpExecutor {
    override fun post(url: String, headers: Map<String, String>, jsonBody: String): TopicPushHttpResult {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
        }
        return try {
            BufferedOutputStream(conn.outputStream).use { out ->
                out.write(jsonBody.toByteArray(StandardCharsets.UTF_8))
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.use { s ->
                BufferedReader(InputStreamReader(s, StandardCharsets.UTF_8)).readText()
            } ?: ""
            TopicPushHttpResult(statusCode = code, body = text)
        } finally {
            conn.disconnect()
        }
    }
}

class HttpTopicPushFcmClient(
    private val endpointUrl: String,
    private val idTokenProvider: () -> String?,
    private val executor: TopicPushHttpExecutor = AndroidTopicPushHttpExecutor()
) : FcmClient {

    override fun sendToTopic(topic: String, title: String, body: String): Boolean {
        val url = endpointUrl.trim()
        if (url.isBlank()) return false
        val token = idTokenProvider() ?: return false
        val json = JSONObject()
            .put("topic", topic)
            .put("title", title)
            .put("body", body)
            .toString()
        val result = executor.post(
            url = url,
            headers = mapOf(
                "Content-Type" to "application/json; charset=utf-8",
                "Authorization" to "Bearer $token"
            ),
            jsonBody = json
        )
        if (result.statusCode !in 200..299) return false
        return parseTopicPushOkResponse(result.body)
    }
}

internal fun parseTopicPushOkResponse(responseBody: String): Boolean {
    if (responseBody.isBlank()) return false
    return try {
        JSONObject(responseBody).optBoolean("ok", false)
    } catch (_: Exception) {
        false
    }
}
