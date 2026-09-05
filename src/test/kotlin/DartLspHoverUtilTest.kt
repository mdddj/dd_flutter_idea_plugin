import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.MarkupContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import shop.itbug.flutterx.util.DartLspHoverUtil

class DartLspHoverUtilTest {

    @Test
    fun parsesTypeFromMarkdownHover() {
        val hover = Hover(
            MarkupContent(
                "markdown",
                """```dart
Map<String, int> value
```
Type: `Map<String, int>`""",
            ),
        )

        assertEquals("Map<String, int>", DartLspHoverUtil.parseHover(hover)?.staticType)
    }

    @Test
    fun parsesTypeFromMarkedStringHover() {
        val hover = Hover(
            listOf(
                org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(
                    """```dart
String value
```""",
                ),
                org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft("Type: `String`"),
            ),
        )

        assertEquals("String", DartLspHoverUtil.parseHover(hover)?.staticType)
    }

    @Test
    fun keepsHoverWithoutTypeForNonTypedSymbols() {
        val hover = Hover(
            MarkupContent("markdown", """```dart
enum Color
```
Declared in `test.dart`."""),
        )

        val result = DartLspHoverUtil.parseHover(hover)
        assertNull(result?.staticType)
        assertNotNull(result)
    }

    @Test
    fun returnsNullForEmptyHover() {
        val hover = Hover(MarkupContent("markdown", ""))

        assertNull(DartLspHoverUtil.parseHover(hover))
    }
}
