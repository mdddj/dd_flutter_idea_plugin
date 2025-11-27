package vm

import com.google.gson.*
import vm.element.WidgetNode

/**
 * 安全的Gson配置，用于处理深度嵌套的Widget Tree
 */
object SafeGsonConfig {
    
    private const val MAX_DEPTH = 250
    
    val gson: Gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .registerTypeAdapter(WidgetNode::class.java, SafeWidgetNodeDeserializer())
        .create()
    
    /**
     * 安全的WidgetNode反序列化器，限制嵌套深度
     */
    class SafeWidgetNodeDeserializer : JsonDeserializer<WidgetNode> {
        
        override fun deserialize(
            json: JsonElement,
            typeOfT: java.lang.reflect.Type,
            context: JsonDeserializationContext
        ): WidgetNode {
            return deserializeWithDepth(json, context, 0)
        }
        
        private fun deserializeWithDepth(
            json: JsonElement,
            context: JsonDeserializationContext,
            currentDepth: Int
        ): WidgetNode {
            val jsonObject = json.asJsonObject
            
            // 如果深度超过限制，返回简化的节点
            if (currentDepth >= MAX_DEPTH) {
                return WidgetNode(
                    description = jsonObject.get("description")?.asString + " (深度限制)",
                    shouldIndent = jsonObject.get("shouldIndent")?.asBoolean,
                    widgetRuntimeType = jsonObject.get("widgetRuntimeType")?.asString,
                    valueId = jsonObject.get("valueId")?.asString,
                    createdByLocalProject = jsonObject.get("createdByLocalProject")?.asBoolean,
                    children = null, // 截断子节点
                    textPreview = jsonObject.get("textPreview")?.asString,
                    properties = null,
                    renderObject = null,
                    hasChildren = jsonObject.get("hasChildren")?.asBoolean,
                    allowsInspection = jsonObject.get("allowsInspection")?.asBoolean,
                    locationId = jsonObject.get("locationId")?.asString,
                    creationLocation = null,
                    isStateful = jsonObject.get("isStateful")?.asBoolean
                )
            }
            
            // 递归处理子节点
            val children = jsonObject.get("children")?.asJsonArray?.let { childrenArray ->
                if (childrenArray.size() > 20) {
                    // 如果子节点太多，只取前20个
                    childrenArray.take(20).map { childElement ->
                        deserializeWithDepth(childElement, context, currentDepth + 1)
                    }
                } else {
                    childrenArray.map { childElement ->
                        deserializeWithDepth(childElement, context, currentDepth + 1)
                    }
                }
            }
            
            return WidgetNode(
                description = jsonObject.get("description")?.asString,
                shouldIndent = jsonObject.get("shouldIndent")?.asBoolean,
                widgetRuntimeType = jsonObject.get("widgetRuntimeType")?.asString,
                valueId = jsonObject.get("valueId")?.asString,
                createdByLocalProject = jsonObject.get("createdByLocalProject")?.asBoolean,
                children = children,
                textPreview = jsonObject.get("textPreview")?.asString,
                properties = null, // 暂时不处理properties以减少复杂度
                renderObject = null,
                hasChildren = jsonObject.get("hasChildren")?.asBoolean,
                allowsInspection = jsonObject.get("allowsInspection")?.asBoolean,
                locationId = jsonObject.get("locationId")?.asString,
                creationLocation = null, // 暂时不处理creationLocation
                isStateful = jsonObject.get("isStateful")?.asBoolean
            )
        }
    }
}