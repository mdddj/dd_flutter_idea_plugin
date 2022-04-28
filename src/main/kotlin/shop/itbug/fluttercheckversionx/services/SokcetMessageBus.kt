package shop.itbug.fluttercheckversionx.services

import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService.SocketResponseModel


/// 低版本中会报错,阉割dio请求监听的功能
//interface SokcetMessageBus {
//    fun handleData(data: SocketResponseModel?)
//
//    companion object {
//        val CHANGE_ACTION_TOPIC = Topic.create("dio request send", SokcetMessageBus::class.java)
//    }
//}