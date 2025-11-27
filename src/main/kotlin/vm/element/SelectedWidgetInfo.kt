package vm.element

import com.google.gson.annotations.SerializedName

/**
 * 选中的Widget信息
 */
data class SelectedWidgetInfo(
    @SerializedName("result")
    val result: SelectedWidgetResult?
)

data class SelectedWidgetResult(
    @SerializedName("objectId")
    val objectId: String?,
    
    @SerializedName("creationLocation")
    val creationLocation: CreationLocation?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("widgetRuntimeType")
    val widgetRuntimeType: String?,
    
    @SerializedName("properties")
    val properties: List<WidgetProperty>?
)