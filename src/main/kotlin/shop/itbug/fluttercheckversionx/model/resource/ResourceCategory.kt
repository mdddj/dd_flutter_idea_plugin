package shop.itbug.fluttercheckversionx.model.resource

import java.io.Serializable

data class ResourceCategory(
    val description: String,
    val id: Int,
    val level: Int,
    val logo: String,
    val name: String,
    val type: String
) : Serializable
