import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import shop.itbug.flutterx.services.FlutterChangelogMarkdownParser

class FlutterChangelogMarkdownParserTest {

    private val parser = FlutterChangelogMarkdownParser()

    @Test
    fun testParseHotfixVersionSection() {
        val result = parser.parseVersion(sampleMarkdown, "3.41.6")

        assertNotNull(result)
        assertEquals("3.41.6", result!!.version)
        assertEquals(
            listOf(
                "flutter/184025 Include a fix from Skia that ensures that the correct atlas for the glyph mask format is used consistently.",
                "flutter/182708 Visual issues with circles appearing jagged. Especially on thin stroked circles and circles with small radii.",
                "flutter/183887 During SCREEN_OFF event a deadlock preventing new frames causing an ANR can occur on android devices running the Android 16 March Security update."
            ),
            result.items
        )
    }

    @Test
    fun testParseMajorReleaseSummarySection() {
        val result = parser.parseVersion(sampleMarkdown, "3.41.0")

        assertNotNull(result)
        assertEquals(
            listOf(
                "Learn about what's new in this release in the blog post, and check out the CHANGELOG for a detailed list of all the new changes."
            ),
            result!!.items
        )
    }

    @Test
    fun testReturnNullWhenVersionMissing() {
        assertNull(parser.parseVersion(sampleMarkdown, "9.9.9"))
    }

    companion object {
        private val sampleMarkdown = """
            In general, our philosophy is to update the `stable` channel on a quarterly basis.
            
            ## Flutter 3.41 Changes
            ### [3.41.6](https://github.com/flutter/flutter/releases/tag/3.41.6)
            - [flutter/184025](https://github.com/flutter/flutter/pull/184025) Include a fix from Skia that ensures that the correct atlas for the glyph mask format is used consistently.
            - [flutter/182708](https://github.com/flutter/flutter/issues/182708) Visual issues with circles appearing jagged. Especially on thin stroked circles and circles with small radii.
            - [flutter/183887](https://github.com/flutter/flutter/issues/183887) During SCREEN_OFF event a deadlock preventing new frames causing an ANR can occur on android devices running the Android 16 March Security update.
            
            ### [3.41.5](https://github.com/flutter/flutter/releases/tag/3.41.5)
            - [flutter/182708](https://github.com/flutter/flutter/issues/182708) When using Impeller on any platform, blur artifacts in circles rendering at 45 degree angles.
            
            ### [3.41.0](https://github.com/flutter/flutter/releases/tag/3.41.0)
            Learn about what's new in this release in [the blog post](https://blog.flutter.dev/whats-new-in-flutter-3-41-302ec140e632), and check out the [CHANGELOG](https://docs.flutter.dev/release/release-notes/release-notes-3.41.0) for a detailed list of all the new changes.
            
            ## Flutter 3.38 Changes
            ### [3.38.5](https://github.com/flutter/flutter/releases/tag/3.38.5)
            - [flutter/179700](https://github.com/flutter/flutter/issues/179700) Update dart to 3.10.4.
        """.trimIndent()
    }
}
