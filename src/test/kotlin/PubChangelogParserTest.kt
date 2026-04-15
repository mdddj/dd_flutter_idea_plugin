
import org.junit.Assert.*
import org.junit.Test
import shop.itbug.flutterx.services.PubChangelogParser

class PubChangelogParserTest {

    private val parser = PubChangelogParser()

    @Test
    fun testParseLatestChangelogEntry() {
        val result = parser.parseLatestChangelog(sampleHtml)

        assertNotNull(result)
        assertEquals("13.0.0", result!!.version)
        assertEquals(expectedContent, result.content)
        assertEquals("13.0.0\n$expectedContent", result.formattedText)
    }

    @Test
    fun testReturnNullWhenNoChangelogEntryFound() {
        assertNull(parser.parseLatestChangelog("<html><body><div>empty</div></body></html>"))
    }

    @Test
    fun testFallbackWhenContainerHasNoChangelogEntryWrapper() {
        val result = parser.parseLatestChangelog(fallbackHtml)

        assertNotNull(result)
        assertEquals("2.0.0", result!!.version)
        assertEquals(
            """
                Added a simplified fallback changelog format.
                
                - Supports pages without explicit changelog-entry wrappers
            """.trimIndent(),
            result.content
        )
    }

    companion object {
        private val expectedContent = """
            > Note: This release has breaking changes.
            
            > Due to an update of win32 to 6.0.0, package requirements were also changed to match this update:
            
            > - Minimum Flutter version is 3.41.6
            > - Minimum Dart version is 3.11.0
            > - Min iOS is 13.0
            > - Min macOS is 10.15
            
            > Since this release was already breaking, the rest of the dependencies were also updated to the latest possible versions.
            
            - BREAKING FEAT(share_plus): Bump win32 from 5.15.0 to 6.0.0 (#3762). (0e3eb918)
        """.trimIndent()

        private val sampleHtml = """
            <!DOCTYPE html>
            <html lang="en-us">
            <body>
            <section class="tab-content detail-tab-changelog-content -active markdown-body">
              <div class="changelog-entry">
                <h2 class="changelog-version hash-header" id="1300">13.0.0 <a href="#1300" class="hash-link">#</a></h2>
                <div class="changelog-content">
                  <blockquote>
                    <p>Note: This release has breaking changes.</p>
                    <p>Due to an update of win32 to 6.0.0, package requirements were also changed to match this update:</p>
                    <ul>
                      <li>Minimum Flutter version is 3.41.6</li>
                      <li>Minimum Dart version is 3.11.0</li>
                      <li>Min iOS is 13.0</li>
                      <li>Min macOS is 10.15</li>
                    </ul>
                    <p>Since this release was already breaking, the rest of the dependencies were also updated to the latest possible versions.</p>
                  </blockquote>
                  <ul>
                    <li><strong>BREAKING</strong> <strong>FEAT</strong>(share_plus): Bump win32 from 5.15.0 to 6.0.0 (<a href="https://github.com/fluttercommunity/plus_plugins/issues/3762" rel="ugc">#3762</a>). (<a href="https://github.com/fluttercommunity/plus_plugins/commit/0e3eb918e77fcc500b6124167a905f026ffc374a" rel="ugc">0e3eb918</a>)</li>
                  </ul>
                </div>
              </div>
              <div class="changelog-entry">
                <h2 class="changelog-version hash-header" id="1202">12.0.2 <a href="#1202" class="hash-link">#</a></h2>
                <div class="changelog-content">
                  <ul>
                    <li><strong>FIX</strong>(share_plus): Avoid crash on iOS during file and text sharing in add-to-app scenario.</li>
                  </ul>
                </div>
              </div>
            </section>
            </body>
            </html>
        """.trimIndent()

        private val fallbackHtml = """
            <!DOCTYPE html>
            <html lang="en-us">
            <body>
            <section class="tab-content detail-tab-changelog-content -active markdown-body">
              <h2>2.0.0</h2>
              <p>Added a simplified fallback changelog format.</p>
              <ul>
                <li>Supports pages without explicit changelog-entry wrappers</li>
              </ul>
              <h2>1.9.0</h2>
              <p>Older release.</p>
            </section>
            </body>
            </html>
        """.trimIndent()
    }
}
