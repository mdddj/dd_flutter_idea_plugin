package shop.itbug.fluttercheckversionx.model.resource

import shop.itbug.fluttercheckversionx.model.user.User

data class MyResource (
    val authority: Long,
    val clickCount: Long,
    val content: String,
    val createDate: String,
    val description: String,
    val id: Long,
    val label: String,
    val links: String,
    val thumbnailImage: String,
    val title: String,
    val type: String,
    val user: User,
    var category: ResourceCategory
)
