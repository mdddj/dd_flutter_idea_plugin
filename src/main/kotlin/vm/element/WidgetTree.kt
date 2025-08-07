package vm.element

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

fun WidgetTreeResponse.getJson() = Gson().toJson(this)

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
    val children: List<WidgetNode>?
)