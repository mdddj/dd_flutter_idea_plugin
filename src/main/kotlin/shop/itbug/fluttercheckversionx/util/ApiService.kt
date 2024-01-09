package shop.itbug.fluttercheckversionx.util

import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate

object ApiService {
    fun getPluginDetail(name: String): PubVersionDataModel? {
        try {
            return ServiceCreate.create<PubService>().callPluginDetails(name).execute().body()
        } catch (e: Exception) {
            jbLog.warn("获取插件失败:${e.localizedMessage}")
            e.printStackTrace()
        }
        return null
    }
}