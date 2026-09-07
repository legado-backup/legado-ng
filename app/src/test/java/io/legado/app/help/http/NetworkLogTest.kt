package io.legado.app.help.http

import okhttp3.Headers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogTest {

    @Test
    fun formatHeadersRedactsAuthenticationCredentials() {
        val headers = Headers.headersOf(
            "Authorization", "Bearer sk-test-secret",
            "Cookie", "session=abc; uid=1",
            "Set-Cookie", "sid=response-secret; Path=/",
            "X-Api-Key", "api-key-secret",
            "X-Goog-Api-Key", "gemini-key-secret",
            "X-CSRF-Token", "csrf-header-secret",
            "X-XSRF-Token", "xsrf-header-secret",
            "User-Agent", "Legado"
        )

        val formatted = NetworkLog.formatHeaders(headers)

        assertTrue(formatted.contains("Authorization: [已脱敏]"))
        assertTrue(formatted.contains("Cookie: [已脱敏]"))
        assertTrue(formatted.contains("Set-Cookie: [已脱敏]"))
        assertTrue(formatted.contains("X-Api-Key: [已脱敏]"))
        assertTrue(formatted.contains("X-Goog-Api-Key: [已脱敏]"))
        assertTrue(formatted.contains("X-CSRF-Token: [已脱敏]"))
        assertTrue(formatted.contains("X-XSRF-Token: [已脱敏]"))
        assertTrue(formatted.contains("User-Agent: Legado"))
        assertFalse(formatted.contains("sk-test-secret"))
        assertFalse(formatted.contains("session=abc"))
        assertFalse(formatted.contains("response-secret"))
        assertFalse(formatted.contains("api-key-secret"))
        assertFalse(formatted.contains("gemini-key-secret"))
        assertFalse(formatted.contains("csrf-header-secret"))
        assertFalse(formatted.contains("xsrf-header-secret"))
    }

    @Test
    fun formatHeaderMapRedactsGeminiApiKey() {
        val formatted = NetworkLog.formatHeaders(
            mapOf(
                "x-goog-api-key" to "gemini-map-secret",
                "Content-Type" to "application/json"
            )
        )

        assertTrue(formatted.contains("x-goog-api-key: [已脱敏]"))
        assertTrue(formatted.contains("Content-Type: application/json"))
        assertFalse(formatted.contains("gemini-map-secret"))
    }

    @Test
    fun redactCredentialsForLogRedactsBodyCredentials() {
        val body = """
            {"api_key":"sk-json-secret","password":"pwd-secret","_csrfToken":"csrf-json-secret","content":"正文 Bearer token 不应出现"}
            access_token=form-secret&xsrf-token=xsrf-form-secret&name=reader
            Authorization: Bearer header-secret
        """.trimIndent()

        val redacted = NetworkLog.redactCredentialsForLog(body)

        assertFalse(redacted.contains("sk-json-secret"))
        assertFalse(redacted.contains("pwd-secret"))
        assertFalse(redacted.contains("form-secret"))
        assertFalse(redacted.contains("csrf-json-secret"))
        assertFalse(redacted.contains("xsrf-form-secret"))
        assertFalse(redacted.contains("header-secret"))
        assertTrue(redacted.contains("\"api_key\":\"[已脱敏]\""))
        assertTrue(redacted.contains("\"password\":\"[已脱敏]\""))
        assertTrue(redacted.contains("\"_csrfToken\":\"[已脱敏]\""))
        assertTrue(redacted.contains("access_token=[已脱敏]"))
        assertTrue(redacted.contains("xsrf-token=[已脱敏]"))
        assertTrue(redacted.contains("Bearer [已脱敏]"))
    }

    @Test
    fun redactUrlForLogRedactsCredentialQueryParams() {
        val url = "https://example.com/api?access_token=url-secret&name=reader" +
            "&api_key=key-secret&_csrfToken=csrf-secret&csrf_token=csrf-snake-secret" +
            "&csrf-token=csrf-kebab-secret&xsrfToken=xsrf-secret#frag"

        val redacted = NetworkLog.redactUrlForLog(url)

        assertEquals(
            "https://example.com/api?access_token=[已脱敏]&name=reader" +
                "&api_key=[已脱敏]&_csrfToken=[已脱敏]&csrf_token=[已脱敏]" +
                "&csrf-token=[已脱敏]&xsrfToken=[已脱敏]#frag",
            redacted
        )
    }

    @Test
    fun redactUrlForLogRedactsOpaqueKeysAndKeepsSearchKeyword() {
        val opaqueKey = "k".repeat(64)
        val protectedUrl = "https://example.com/api?key=$opaqueKey&lang=zh"
        val searchUrl = "https://example.com/search?key=三体&page=1"

        assertEquals(
            "https://example.com/api?key=[已脱敏]&lang=zh",
            NetworkLog.redactUrlForLog(protectedUrl),
        )
        assertEquals(searchUrl, NetworkLog.redactUrlForLog(searchUrl))
    }

    @Test
    fun redactThrowableForLogDropsUrlKeyCookieAndOriginalCause() {
        val opaqueKey = "k".repeat(64)
        val cookieValue = "fixture-cookie-secret"
        val error = IllegalStateException(
            "request failed https://example.com/api?key=$opaqueKey " +
                "headers={\"Cookie\":\"session=$cookieValue; lang=zh\"}"
        ).apply {
            stackTrace = arrayOf(StackTraceElement("Fixture", "call", "Fixture.kt", 12))
        }

        val redacted = NetworkLog.redactThrowableForLog(error)
        val text = redacted.stackTraceToString()

        assertFalse(text.contains(opaqueKey))
        assertFalse(text.contains(cookieValue))
        assertTrue(text.contains("key=[已脱敏]"))
        assertTrue(text.contains("\"Cookie\":\"[已脱敏]\""))
        assertTrue(text.contains("Fixture.call(Fixture.kt:12)"))
        assertNull(redacted.cause)
    }

    @Test
    fun displaySourceUsesGlobalWhenSourceIsMissing() {
        assertEquals("全局", NetworkLog.displaySource(null))
        assertEquals("全局", NetworkLog.displaySource(""))
        assertEquals("书源 / 书名 / 章节", NetworkLog.displaySource("书源 / 书名 / 章节"))
    }
}
