package shop.itbug.fluttercheckversionx.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class BlogCategory (
    val createTime: String,
    val id: Long,
    val intro: String,
    val logo: String,
    val name: String
)

@Serializable
data class BlogWriteModel (
    var title: String = "",
    var content: String = "",
    var tags: List<String> = emptyList(),

    @SerialName("categoryId")
    var categoryID: Long? = null,

    var alias: String = "",
    var thumbnail: String = "",
    var id: Long? = null
)