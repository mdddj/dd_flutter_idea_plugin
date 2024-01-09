package shop.itbug.fluttercheckversionx.socket.service

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.application.CachedSingletonsRegistry
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.extension.plugins.HeartPlugin
import org.smartboot.socket.extension.processor.AbstractMessageProcessor
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import org.smartboot.socket.transport.WriteBuffer
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol
import shop.itbug.fluttercheckversionx.util.jbLog
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

@Service(Service.Level.APP)
class DioApiService {

    companion object {
        //        val INSTANCESupplier: Supplier<DioApiService> = CachedSingletonsRegistry.lazy { service<DioApiService>() }
        val INSTANCESupplierSupplier: Supplier<DioApiService> =
            CachedSingletonsRegistry.lazy { service<DioApiService>() }

    }

    fun get() = INSTANCESupplierSupplier.get()


    private val sessions = mutableSetOf<AioSession>()
    private val messageProcessor = MyMessageProcessor

    ///添加消息处理程序
    fun addHandle(processor: NativeMessageProcessing) {
        messageProcessor.addHandle(processor)
    }

    fun getSessions() = sessions

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

    fun sendByMap(map: MutableMap<String, Any>) {
        sendMessage(JSONObject.toJSONString(map))
    }

    fun sendByAnyObject(obj: Any) {
        val json = JSON.toJSONString(obj)
        sendByMap(JSON.parseObject(json))
    }


    interface NativeMessageProcessing {


        fun register() {
            INSTANCESupplierSupplier.get().get().addHandle(this)
        }

        /**
         * 处理 app 那边发过来的消息
         * @param nativeMessage 字符串消息
         * @param jsonObject 会尝试转换字符串消息为JSON,注意判空
         */
        fun handleFlutterAppMessage(nativeMessage: String, jsonObject: JSONObject?, aio: AioSession?)

        fun stateEvent(
            session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?
        ) {
        }
    }


    ///flutter api 接口模型
    interface HandleFlutterApiModel : NativeMessageProcessing {
        fun handleModel(model: ProjectSocketService.SocketResponseModel) {}

        fun covertJsonError(e: Exception, aio: AioSession?) {}

        override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: JSONObject?, aio: AioSession?) {
            try {
                jsonObject?.to(ProjectSocketService.SocketResponseModel::class.java)?.let {
                    if (it.projectName.isNotBlank()) {
                        handleModel(it)
                    }
                }
            } catch (e: Exception) {
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
            val json = JSONObject.parse(msg)
            handle.forEach {
                it.handleFlutterAppMessage(msg, json, session)
            }
        } catch (_: Exception) {
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
        defaultEventHandle(session, stateMachineEnum)
        super.stateEvent(session, stateMachineEnum, throwable)
    }


    private fun defaultEventHandle(session: AioSession?, stateMachineEnum: StateMachineEnum?) {
        jbLog.info("监听状态发现变化:$session $stateMachineEnum")
        when (stateMachineEnum) {
            StateMachineEnum.NEW_SESSION -> {
                DioApiService.INSTANCESupplierSupplier.get().get().addSession(session)
            }

            StateMachineEnum.INPUT_SHUTDOWN -> DioApiService.INSTANCESupplierSupplier.get().get().removeSession(session)
            StateMachineEnum.SESSION_CLOSING -> DioApiService.INSTANCESupplierSupplier.get().get()
                .removeSession(session)

            StateMachineEnum.SESSION_CLOSED -> DioApiService.INSTANCESupplierSupplier.get().get().removeSession(session)
            StateMachineEnum.REJECT_ACCEPT -> DioApiService.INSTANCESupplierSupplier.get().get().removeSession(session)
            StateMachineEnum.ACCEPT_EXCEPTION -> DioApiService.INSTANCESupplierSupplier.get().get()
                .removeSession(session)

            else -> {}
        }
    }


}


///连接心跳插件.
private class MyHeartCommon : HeartPlugin<String>(15, TimeUnit.SECONDS) {
    override fun sendHeartRequest(session: AioSession?) {
        session?.send("ping")
    }

    override fun isHeartMessage(session: AioSession?, msg: String?): Boolean {
        val isHeartMessage = msg != null && msg == "ping"
        jbLog.info("是否为心跳消息:$isHeartMessage")
        return isHeartMessage
    }

}


private fun AioSession.send(message: String) {
    try {
        val writeBuffer: WriteBuffer = writeBuffer()
        val data = message.toByteArray()
        writeBuffer.write(data)
        writeBuffer.flush()
    } catch (e: Exception) {
        jbLog.warn("发送消息失败:$e")
    }
}