package vm.element

import com.google.gson.annotations.SerializedName


data class FlutterInspectorGetPropertiesResponse(
    @SerializedName("result")
    val result: List<Property>,

    @SerializedName("type")
    val type: String,

    @SerializedName("method")
    val method: String
)

data class CreationLocation(
    @SerializedName("file")
    val file: String,

    @SerializedName("line")
    val line: Int,

    @SerializedName("column")
    val column: Int,

    @SerializedName("name")
    val name: String
)

data class Property(
    @SerializedName("description")
    val description: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("level")
    val level: String,

    @SerializedName("style")
    val style: String,

    @SerializedName("allowNameWrap")
    val allowNameWrap: Boolean,

    @SerializedName("valueId")
    val valueId: String?,

    @SerializedName("ifNull")
    val ifNull: String?,

    @SerializedName("missingIfNull")
    val missingIfNull: Boolean,

    @SerializedName("propertyType")
    val propertyType: String,

    @SerializedName("defaultLevel")
    val defaultLevel: String,

    // 'value' 可以是 Int, Boolean, String 或 null, 所以用 Any?
    @SerializedName("value")
    val value: Any?,

    @SerializedName("locationId")
    val locationId: Int?,

    @SerializedName("creationLocation")
    val creationLocation: CreationLocation?,

    @SerializedName("createdByLocalProject")
    val createdByLocalProject: Boolean?,

    @SerializedName("defaultValue")
    val defaultValue: Any?,

    @SerializedName("isDiagnosticableValue")
    val isDiagnosticableValue: Boolean?,

    @SerializedName("showName")
    val showName: Boolean?,

    @SerializedName("ifTrue")
    val ifTrue: String?
)