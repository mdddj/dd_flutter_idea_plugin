import org.junit.Test
import vm.element.WidgetNode

class WidgetTreeTest {
    
    @Test
    fun testWidgetNodeWithTextPreview() {
        val textWidget = WidgetNode(
            description = "Text",
            shouldIndent = false,
            widgetRuntimeType = "Text",
            valueId = "test-id",
            createdByLocalProject = true,
            children = null,
            textPreview = "Hello World",  // 这就是Text widget的文本内容
            properties = null,
            renderObject = null,
            hasChildren = false,
            allowsInspection = true,
            locationId = null,
            creationLocation = null,
            isStateful = false
        )
        
        assert(textWidget.textPreview == "Hello World")
        assert(textWidget.widgetRuntimeType == "Text")
        
        println("✅ WidgetNode with textPreview test passed")
        println("Text content: ${textWidget.textPreview}")
    }
}