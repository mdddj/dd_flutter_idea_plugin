package shop.itbug.fluttercheckversionx.services

import PluginVersionModel
import com.intellij.util.io.HttpRequests
import kotlinx.serialization.json.Json
import shop.itbug.fluttercheckversionx.model.PubSearchResult
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel


/**
 * 访问pub开放Api接口
 * 接口url: [https://pub.dartlang.org/api/packages/插件名字]
 */
object PubService {


    /**
     * 获取插件的相关数据
     */
    fun callPluginDetails(plugName: String): PubVersionDataModel? {
        val url = "https://pub.dartlang.org/api/packages/$plugName"
        try {
            val resposne = HttpRequests.request(url).readString()
            return Json.decodeFromString(resposne)
        } catch (e: Exception) {
            println("获取插件信息失败${plugName}:${e.message} url:$url")
            return null
        }
    }

    /**
     * 获取包的版本列表
     */
    fun getPackageVersions(pluginName: String): PluginVersionModel? {
        val url = "https://pub.dartlang.org/packages/$pluginName.json"
        try {
            val resposne = HttpRequests.request(url).readString()
            return Json.decodeFromString(resposne)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 搜索包
     */
    fun search(pluginName: String): PubSearchResult? {
        val url = "https://pub.dartlang.org/api/search?q=$pluginName"
        try {
            val resposne = HttpRequests.request(url).readString()
            return Json.decodeFromString(resposne)
        } catch (e: Exception) {
            return null
        }
    }
}


