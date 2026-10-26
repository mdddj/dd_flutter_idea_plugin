import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import shop.itbug.flutterx.common.MAX_ISSUE_BODY_LENGTH
import shop.itbug.flutterx.common.MAX_ISSUE_TITLE_LENGTH
import shop.itbug.flutterx.common.buildIssueTitle
import shop.itbug.flutterx.common.buildIssueUrl
import shop.itbug.flutterx.common.headlineOf
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class PluginErrorHandleTest {

    private val baseUrl = "https://github.com/mdddj/dd_flutter_idea_plugin/issues/new"

    @Test
    fun `headline derives a single line summary`() {
        val error = IllegalStateException("boom")

        assertEquals("IllegalStateException: boom", headlineOf(null, error))
        assertEquals(
            "java.lang.IllegalStateException: boom",
            headlineOf("java.lang.IllegalStateException: boom", error)
        )
        assertEquals("IllegalStateException: custom text", headlineOf("custom text", error))
        assertEquals("java.lang.IllegalStateException", headlineOf(null, IllegalStateException()))
        assertEquals("just a message", headlineOf("just a message", null))
        assertEquals("Unknown error", headlineOf("   ", null))
    }

    @Test
    fun `title is single line and capped`() {
        assertEquals("[FlutterX] boom at line", buildIssueTitle("FlutterX", "boom\nat   line"))
        assertEquals("[FlutterX] Unknown error", buildIssueTitle("FlutterX", " "))

        val long = buildIssueTitle("FlutterX", "x".repeat(500))
        assertEquals(MAX_ISSUE_TITLE_LENGTH, long.length)
        assertTrue(long.endsWith("…"))
    }

    @Test
    fun `url carries decoded title and body`() {
        val url = buildIssueUrl(baseUrl, "a title", "body with & and = signs")
        val query = url.substringAfter('?')

        assertEquals(baseUrl, url.substringBefore('?'))
        assertEquals("a title", decode(query.substringAfter("title=").substringBefore('&')))
        assertEquals("body with & and = signs", decode(query.substringAfter("body=")))
    }

    @Test
    fun `long body is cut on an encoded boundary and flagged`() {
        val body = "错误堆栈".repeat(4000)
        val encodedBody = buildIssueUrl(baseUrl, "t", body).substringAfter("body=")

        assertTrue("encoded body must stay within budget", encodedBody.length <= MAX_ISSUE_BODY_LENGTH)
        val decoded = decode(encodedBody)
        val note = "\n\n> Report truncated. The full text has been copied to your clipboard."
        assertTrue("truncated report must point at the clipboard", decoded.endsWith(note))
        val kept = decoded.removeSuffix(note)
        assertTrue("kept text must be a clean prefix", body.startsWith(kept))
        assertTrue("kept text must not be empty", kept.isNotEmpty())
    }

    @Test
    fun `short body is passed through untouched`() {
        val decoded = decode(buildIssueUrl(baseUrl, "t", "small").substringAfter("body="))
        assertEquals("small", decoded)
    }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)
}
