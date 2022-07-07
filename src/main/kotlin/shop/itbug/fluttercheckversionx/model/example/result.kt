package shop.itbug.fluttercheckversionx.model.example


data class ExampleResult (
    val data: List<ResourceModel>,
    val message: String,
    val state: Long
)

data class ResourceModel (
    val authority: Long,
    val category: CategoryModel,
    val clickCount: Long,
    val content: String,
    val createDate: Long,
    val description: String,
    val fileInfo: Any? = null,
    val id: Long,
    val images: List<Any?>,
    val label: String,
    val links: String,
    val mianji: Any? = null,
    val thumbnailImage: String,
    val title: String,
    val type: String,
    val updateDate: Any? = null,
)

data class CategoryModel (
    val announcement: Any? = null,
    val description: String,
    val id: Long,
    val level: Long,
    val logo: String,
    val name: String,
    val navJSONString: String,
    val parentNode: ParentNode,
    val type: String,
    val users: List<Any?>
)

data class ParentNode (
    val announcement: Any? = null,
    val description: String,
    val id: Long,
    val level: Long,
    val logo: String,
    val name: String,
    val navJSONString: String,
    val parentNode: ParentNode? = null,
    val type: String,
    val users: List<Any?>
)

