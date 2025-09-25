package shop.itbug.fluttercheckversionx.services

import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.model.*
import shop.itbug.fluttercheckversionx.socket.service.DioApiService


/**
 * 访问pub开放Api接口
 * 接口url: [https://pub.dartlang.org/api/packages/插件名字]
 */
object PubService {


    fun getApiUrl(pluginName: String): String {
        return "${DioListingUiConfig.setting.pubServerUrl}/api/packages/$pluginName"
    }

    /**
     * 获取插件的相关数据
     */
    fun callPluginDetails(plugName: String): PubVersionDataModel? {
        val url = "${DioListingUiConfig.setting.pubServerUrl}/api/packages/$plugName"
        try {
            val resposne = HttpRequests.request(url).readString()
            return DioApiService.getInstance().gson.fromJson(resposne, PubVersionDataModel::class.java)
                .copy(jsonText = resposne)
        } catch (e: Exception) {
            println("获取插件信息失败${plugName} url:$url  ${e.localizedMessage}")
            return null
        }
    }

    /**
     * 获取包的版本列表
     */
    fun getPackageVersions(pluginName: String): PluginVersionModel? {
        val url = "${DioListingUiConfig.setting.pubServerUrl}/packages/$pluginName.json"
        try {
            val resposne = HttpRequests.request(url).readString()
            return DioApiService.getInstance().gson.fromJson(resposne, PluginVersionModel::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 搜索包
     */
    fun search(pluginName: String): PubSearchResult? {
        val url = "${DioListingUiConfig.setting.pubServerUrl}/api/search?q=$pluginName"
        try {
            val resposne = HttpRequests.request(url).readString()
            return DioApiService.getInstance().gson.fromJson(resposne, PubSearchResult::class.java)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * 获取插件评分
     */
    fun getScore(pluginName: String): PubPackageScore? {
        val url = "${DioListingUiConfig.setting.pubServerUrl}/api/packages/$pluginName/score"
        return try {
            DioApiService.getInstance().gson.fromJson(
                HttpRequests.request(url).readString(),
                PubPackageScore::class.java
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取插件详情
     */
    fun findAllPluginInfo(packageNames: List<String>): List<PubPackageInfo> {
        val r = runBlocking(Dispatchers.IO) {
            return@runBlocking packageNames.map {
                async {
                    return@async getPubPackageInfoModel(it)
                }
            }.awaitAll()
        }.filterNotNull()
        return r
    }

    fun getPubPackageInfoModel(name: String): PubPackageInfo? {
        val score = getScore(name) ?: return null
        val info = callPluginDetails(name) ?: return null
        return PubPackageInfo(score, info)
    }
}


