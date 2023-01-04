package shop.itbug.fluttercheckversionx.model.resource

import com.alibaba.fastjson2.JSONObject
import com.intellij.util.xmlb.Converter
import java.io.Serializable

data class ResourceCategory(
    val description: String,
    val id: Int,
    val level: Int,
    val logo: String,
    val name: String,
    val type: String
) : Serializable


class ResourceCategoryCovert : Converter<ResourceCategory>() {
    override fun toString(value: ResourceCategory): String? {
        return JSONObject.toJSONString(value)
    }

    override fun fromString(value: String): ResourceCategory? {
        return JSONObject.parseObject(value,ResourceCategory::class.java)
    }

}