package shop.itbug.fluttercheckversionx.socket.service

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.extension.processor.AbstractMessageProcessor
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import org.smartboot.socket.transport.WriteBuffer
import shop.itbug.fluttercheckversionx.listeners.MyLoggerEvent
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol
import shop.itbug.fluttercheckversionx.socket.service.DioApiService.Companion.getInstance
import shop.itbug.fluttercheckversionx.window.logger.LogKeys
import shop.itbug.fluttercheckversionx.window.logger.MyLogInfo
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class DioApiService {

    companion object {
        fun getInstance() = service<DioApiService>()
    }


    private val sessions = mutableSetOf<AioSession>()
    private val messageProcessor = MyMessageProcessor

    ///添加消息处理程序
    fun addHandle(processor: NativeMessageProcessing) {
        messageProcessor.addHandle(processor)
    }

    fun addSession(session: AioSession?) {
        session?.let { sessions.add(it) }
    }

    fun removeSession(session: AioSession?) {
        session?.let { sessions.remove(it) }
    }

    private fun sendMessage(message: String) {
        sessions.forEach {
            it.send(message)
        }
    }

    fun sendByMap(map: Map<String, Any>) {
        sendMessage(Gson().toJson(map))
    }

    fun sendByAnyObject(obj: Any) {
        try {
            val json: String = Gson().toJson(obj)
            val map = Gson().fromJson(json, Map::class.java)
            sendByMap(map.toMapAny)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    interface NativeMessageProcessing {


        fun register() {
            getInstance().addHandle(this)
        }

        /**
         * 处理 app 那边发过来的消息
         * @param nativeMessage 字符串消息
         * @param jsonObject 会尝试转换字符串消息为JSON,注意判空
         */
        fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?)

        fun stateEvent(
            session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?
        ) {
        }
    }


    ///flutter api 接口模型
    interface HandleFlutterApiModel : NativeMessageProcessing {
        fun handleModel(model: ProjectSocketService.SocketResponseModel) {}

        fun covertJsonError(e: Exception, aio: AioSession?) {}

        override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
            try {
                if (jsonObject != null && jsonObject["projectName"] != null) {
                    val model = Gson().fromJson(nativeMessage, ProjectSocketService.SocketResponseModel::class.java)
                    println("-----start")
                    println(model)
                    println("-----end")
                    handleModel(model)
                }
            } catch (e: Exception) {
                println("转换异常:${e.localizedMessage}")
                e.printStackTrace()
                covertJsonError(e, aio)
            }
        }
    }

    fun builder(port: Int): AioQuickServer {
        val server = AioQuickServer(port, StringProtocol(), messageProcessor)
        server.setBannerEnabled(false)
        server.setReadBufferSize(10485760 * 2)
        return server
    }


}

object MyMessageProcessor : AbstractMessageProcessor<String>() {

    private var handle = setOf<DioApiService.NativeMessageProcessing>()

    init {
        addPlugin(MyHeartCommon())
    }

    fun addHandle(processor: DioApiService.NativeMessageProcessing) {
        handle = handle.plus(processor)
    }


    private fun jsonStringToModel(msg: String, session: AioSession?) {
        try {
            val json = Gson().fromJson(msg, Map::class.java).toMapAny
            handle.forEach {
                it.handleFlutterAppMessage(msg, json, session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            handle.forEach {
                it.handleFlutterAppMessage(msg, null, session)
            }
        }
    }

    override fun process0(session: AioSession?, msg: String?) {
        if (msg != null) {
            jsonStringToModel(msg, session)
        }
    }

    override fun stateEvent0(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {
        handle.forEach {
            it.stateEvent(session, stateMachineEnum, throwable)
        }
        throwable?.let {
            throwable.printStackTrace()
        }
        defaultEventHandle(session, stateMachineEnum)
    }


    private fun defaultEventHandle(session: AioSession?, stateMachineEnum: StateMachineEnum?) {
        println("==$session $stateMachineEnum")
        when (stateMachineEnum) {
            StateMachineEnum.NEW_SESSION -> getInstance().addSession(session)
            StateMachineEnum.INPUT_SHUTDOWN -> getInstance().removeSession(session)
            StateMachineEnum.SESSION_CLOSING -> getInstance().removeSession(session)
            StateMachineEnum.SESSION_CLOSED -> getInstance().removeSession(session)
            StateMachineEnum.REJECT_ACCEPT -> getInstance().removeSession(session)
            StateMachineEnum.ACCEPT_EXCEPTION -> getInstance().removeSession(session)
            else -> {}
        }
    }


}


///连接心跳插件.
private class MyHeartCommon : MyHeartPlugin<String>(15, TimeUnit.SECONDS) {
    override fun sendHeartRequest(session: AioSession?) {
        session?.send("ping")
    }


    override fun isHeartMessage(session: AioSession?, msg: String?): Boolean {
        msg?.let {
            val json = Gson().fromJson(it, HashMap::class.java)
            if (json["type"] == "ping") {
                MyLoggerEvent.fire(MyLogInfo(message = "$msg", key = LogKeys.ping))
                return true
            }
        }
        return false
    }

}


private fun AioSession.send(message: String) {
    try {
        val writeBuffer: WriteBuffer = writeBuffer()
        val data = message.toByteArray()
        writeBuffer.write(data)
        writeBuffer.flush()
    } catch (e: Exception) {
        println("发送消息失败:$e")
    }
}

val Map<*, *>.toMapAny: Map<String, Any>
    get() {
        val map = mutableMapOf<String, Any>()
        this.forEach { (k, v) ->
            run {
                if (v != null) {
                    map[k.toString()] = v
                }
            }
        }
        return map
    }