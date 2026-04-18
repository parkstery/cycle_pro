package com.rtw.pro.notification.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpTopicPushFcmClientTest {

    @Test
    fun sendToTopic_returnsFalseWhenNoIdToken() {
        val client = HttpTopicPushFcmClient(
            endpointUrl = "https://example.com/sendTopicPush",
            idTokenProvider = { null },
            executor = TopicPushHttpExecutor { _, _, _ ->
                TopicPushHttpResult(200, """{"ok":true}""")
            }
        )
        assertFalse(client.sendToTopic("daily-20h", "t", "b"))
    }

    @Test
    fun sendToTopic_returnsTrueOn200AndOkJson() {
        val client = HttpTopicPushFcmClient(
            endpointUrl = "https://example.com/sendTopicPush",
            idTokenProvider = { "fake-token" },
            executor = TopicPushHttpExecutor { _, headers, jsonBody ->
                assertTrue(headers["Authorization"]!!.startsWith("Bearer "))
                assertTrue(jsonBody.contains("daily-20h"))
                TopicPushHttpResult(200, """{"ok":true}""")
            }
        )
        assertTrue(client.sendToTopic("daily-20h", "t", "b"))
    }

    @Test
    fun sendToTopic_returnsFalseOnNon2xx() {
        val client = HttpTopicPushFcmClient(
            endpointUrl = "https://example.com/sendTopicPush",
            idTokenProvider = { "fake-token" },
            executor = TopicPushHttpExecutor { _, _, _ ->
                TopicPushHttpResult(401, """{"ok":false}""")
            }
        )
        assertFalse(client.sendToTopic("daily-20h", "t", "b"))
    }
}

class ParseTopicPushOkResponseTest {
    @Test
    fun parse_ok() {
        assertTrue(parseTopicPushOkResponse("""{"ok":true}"""))
    }

    @Test
    fun parse_missingOk() {
        assertFalse(parseTopicPushOkResponse("""{"ok":false}"""))
        assertFalse(parseTopicPushOkResponse("{}"))
        assertFalse(parseTopicPushOkResponse(""))
    }
}
