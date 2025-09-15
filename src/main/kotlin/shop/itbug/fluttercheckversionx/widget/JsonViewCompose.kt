package shop.itbug.fluttercheckversionx.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonElement
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

sealed class JsonTreeNode {
    data class ValueNode(
        val key: String,
        val value: String,
        val style: TextStyle
    ) : JsonTreeNode()

    data class ObjectNode(
        val key: String,
        val children: List<JsonTreeNode>
    ) : JsonTreeNode()

    data class ArrayNode(
        val key: String,
        val children: List<JsonTreeNode>
    ) : JsonTreeNode()

    fun getNodeKey(): String {
        return when (this) {
            is ValueNode -> key
            is ObjectNode -> key
            is ArrayNode -> key
        }
    }
}

@Composable
fun GsonJsonTree(jsonElement: JsonElement) {
    val expandedStates = remember { mutableStateMapOf<String, MutableState<Boolean>>() }
    val rootNode = remember(jsonElement) { convertGsonToTree(jsonElement) }

    Column(modifier = Modifier.padding(8.dp)) {
        JsonTreeNodeItem(
            node = rootNode,
            depth = 0,
            expandedStates = expandedStates,
            path = "" // 根路径
        )
    }
}

@Composable
private fun JsonTreeNodeItem(
    node: JsonTreeNode,
    depth: Int,
    expandedStates: SnapshotStateMap<String, MutableState<Boolean>>,
    path: String
) {
    when (node) {
        is JsonTreeNode.ValueNode -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 16).dp)
            ) {
                Text(
                    text = "${node.getNodeKey()}: ",
                    style = TextStyle(
                        color = Color(0xFF9B44FF),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = node.value,
                    style = node.style
                )
            }
        }

        is JsonTreeNode.ObjectNode -> {
            val currentPath = if (path.isEmpty()) node.getNodeKey() else "$path.${node.getNodeKey()}"
            var isExpanded by expandedStates
                .getOrPut(currentPath) { mutableStateOf(false) }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedStates[currentPath]?.value = !isExpanded }
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.width(((depth * 16)-2).dp)) {
                        Icon(
                            key = if (isExpanded)
                                AllIconsKeys.General.ArrowDown
                            else
                                AllIconsKeys.General.ArrowRight,
                            contentDescription = "Toggle",
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.CenterEnd)
                        )
                    }

                    if (node.getNodeKey().isNotEmpty()) {
                        Text(
                            text = "${node.getNodeKey()}: ",
                            style = TextStyle(
                                color = Color(0xFF9B44FF),
                                fontWeight = FontWeight.Bold
                            ),
                        )
                    }

                    Text(
                        text = "{...} (${node.children.size} items)",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    )
                }

                if (isExpanded) {
                    node.children.forEachIndexed { index, nodeItem: JsonTreeNode ->
                        val childPath = if (node.getNodeKey().isEmpty())
                            "[${index}]"
                        else if (nodeItem is JsonTreeNode.ValueNode || nodeItem is JsonTreeNode.ObjectNode)
                            "$currentPath . ${nodeItem.getNodeKey()}"
                        else
                            "$currentPath[${index}]"

                        JsonTreeNodeItem(
                            node = nodeItem,
                            depth = depth + 1,
                            expandedStates = expandedStates,
                            path = childPath
                        )
                    }
                }
            }
        }

        is JsonTreeNode.ArrayNode -> {
            val currentPath = if (path.isEmpty()) node.getNodeKey() else "$path.${node.getNodeKey()}"
            val isExpanded by expandedStates
                .getOrPut(currentPath) { mutableStateOf(false) }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedStates[currentPath]?.value = !isExpanded }
                        .padding(vertical = 4.dp)
                ) {

                    Box(modifier = Modifier.width(((depth * 16)-2).dp)) {
                        Icon(
                            key = if (isExpanded)
                                AllIconsKeys.General.ArrowDown
                            else
                                AllIconsKeys.General.ArrowRight,
                            contentDescription = "Toggle",
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.CenterEnd)
                        )
                    }

                    if (node.getNodeKey().isNotEmpty()) {
                        Text(
                            text = "${node.getNodeKey()}: ",
                            style = TextStyle(
                                color = Color(0xFF9B44FF),
                                fontWeight = FontWeight.Bold
                            ),
                        )
                    }

                    Text(
                        text = "[...] (${node.children.size} items)",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    )
                }

                if (isExpanded) {
                    node.children.forEachIndexed { index, child ->
                        val childPath = "$currentPath[$index]"
                        JsonTreeNodeItem(
                            node = child,
                            depth = depth + 1,
                            expandedStates = expandedStates,
                            path = childPath
                        )
                    }
                }
            }
        }
    }
}

private fun convertGsonToTree(
    element: JsonElement,
    key: String = ""
): JsonTreeNode {
    return when {
        element.isJsonObject -> {
            val jsonObject = element.asJsonObject
            val children = jsonObject.entrySet().map { (k, v) ->
                convertGsonToTree(v, k)
            }
            JsonTreeNode.ObjectNode(key, children)
        }

        element.isJsonArray -> {
            val jsonArray = element.asJsonArray
            val children = jsonArray.mapIndexed { index, item ->
                convertGsonToTree(item, "[$index]")
            }
            JsonTreeNode.ArrayNode(key, children)
        }

        element.isJsonPrimitive -> {
            val primitive = element.asJsonPrimitive
            val (value, style) = when {
                primitive.isString -> {
                    val stringValue = primitive.asString
                    stringValue to TextStyle(color = Color(0xFFE67E22))
                }

                primitive.isBoolean -> {
                    primitive.asBoolean.toString() to TextStyle(color = Color(0xFF2ECC71))
                }

                primitive.isNumber -> {
                    primitive.asNumber.toString() to TextStyle(color = Color(0xFF27AE60))
                }

                else -> {
                    primitive.asString to TextStyle(color = Color.Gray)
                }
            }
            JsonTreeNode.ValueNode(key, value, style)
        }

        element.isJsonNull -> {
            JsonTreeNode.ValueNode(key, "null", TextStyle(color = Color.Gray))
        }

        else -> {
            JsonTreeNode.ValueNode(key, "unknown", TextStyle(color = Color.Red))
        }
    }
}

// 使用示例
@Composable
fun JsonViewerDemo() {
    // 您的JSON字符串
    val jsonString = """{
  "slideshow": {
    "author": "Yours Truly",
    "date": "date of publication",
    "slides": [
      {
        "title": "Wake up to WonderWidgets!",
        "type": "all"
      },
      {
        "items": [],
        "title": "Overview",
        "type": "all"
      }
    ]
  }
}"""

    // 使用Gson解析
    val gson = Gson()
    val jsonElement = gson.fromJson(jsonString, JsonElement::class.java)

    GsonJsonTree(jsonElement)
}