package shop.itbug.fluttercheckversionx.util

import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate

object ApiService {
    fun getPluginDetail(name: String): PubVersionDataModel? {
        val response = ServiceCreate.create<PubService>().callPluginDetails(name).execute()
        if (response.isSuccessful) {
            val detail = response.body()
            detail?.let {
                return it
            }
        }
        return null
    }
}