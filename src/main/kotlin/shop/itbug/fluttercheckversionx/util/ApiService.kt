package shop.itbug.fluttercheckversionx.util

import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.window.logger.LogKeys
import shop.itbug.fluttercheckversionx.window.logger.MyLogInfo
import java.time.LocalDateTime

object ApiService {
    fun getPluginDetail(name: String): PubVersionDataModel? {
        try {
            return PubService.callPluginDetails(name)
        } catch (e: Exception) {
            println("获取插件失败:${e.localizedMessage}")
            MyLoggerEvent.fire(
                MyLogInfo(
                    message = "${LocalDateTime.now()} [$name] Failed to detect package version: ${e.localizedMessage}",
                    key = LogKeys.checkPlugin
                )
            )
            e.printStackTrace()
        }
        return null
    }
}