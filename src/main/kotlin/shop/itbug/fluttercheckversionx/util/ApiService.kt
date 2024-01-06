package shop.itbug.fluttercheckversionx.util

import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.tools.log

object ApiService {
    fun getPluginDetail(name: String): PubVersionDataModel? {
        try {
            return ServiceCreate.create<PubService>().callPluginDetails(name).execute().body()
        } catch (e: Exception) {
            log.warn("获取插件失败:${e.localizedMessage}")
        }
        return null
    }
}