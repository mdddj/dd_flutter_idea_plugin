package vm.element

import com.google.gson.annotations.SerializedName

data class WidgetTreeResponse(
    @SerializedName("result")
    val result: WidgetNode?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("method")
    val method: String?
)


data class WidgetNode(
    @SerializedName("description")
    val description: String?,

    @SerializedName("shouldIndent")
    val shouldIndent: Boolean?,

    @SerializedName("widgetRuntimeType")
    val widgetRuntimeType: String?,

    @SerializedName("valueId")
    val valueId: String?,

    @SerializedName("createdByLocalProject")
    val createdByLocalProject: Boolean?,

    @SerializedName("children")
    val children: List<WidgetNode>?,

    // 新增字段用于显示Text内容和其他详细信息
    @SerializedName("textPreview")
    val textPreview: String?,

    @SerializedName("properties")
    val properties: List<WidgetProperty>?,

    @SerializedName("renderObject")
    val renderObject: RenderObjectInfo?,

    @SerializedName("hasChildren")
    val hasChildren: Boolean?,

    @SerializedName("allowsInspection")
    val allowsInspection: Boolean?,

    @SerializedName("locationId")
    val locationId: String?,

    @SerializedName("creationLocation")
    val creationLocation: CreationLocation?,

    @SerializedName("isStateful")
    val isStateful: Boolean?
)

data class WidgetProperty(
    @SerializedName("name")
    val name: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("value")
    val value: String?,

    @SerializedName("level")
    val level: String?
)

data class RenderObjectInfo(
    @SerializedName("description")
    val description: String?,

    @SerializedName("properties")
    val properties: List<WidgetProperty>?
)
/*
* 表示来自 VM 服务（如 flutter.inspector）的位置信息。
*
* @property fileUri 文件的 URI 路径。
* @property line 文件中的行号。
* @property column 文件中的列号。
* @property source 事件的来源标识。
*/
data class NavigatorLocationInfo(
    @SerializedName("fileUri")
    val fileUri: String,

    @SerializedName("line")
    val line: Int,

    @SerializedName("column")
    val column: Int,

    @SerializedName("source")
    val source: String
)