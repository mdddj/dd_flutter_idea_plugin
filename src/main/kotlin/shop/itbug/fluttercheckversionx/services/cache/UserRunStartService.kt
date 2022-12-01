package shop.itbug.fluttercheckversionx.services.cache

import com.intellij.openapi.components.service
import shop.itbug.fluttercheckversionx.socket.service.AppService

/**
 * 在idea启动的时候,实现自动登录的功能
 */
class UserRunStartService: Runnable {

    override fun run() {
       service<AppService>().login()
    }
}