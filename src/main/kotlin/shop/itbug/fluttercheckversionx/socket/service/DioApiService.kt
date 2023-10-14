package shop.itbug.fluttercheckversionx.socket.service

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import org.smartboot.socket.transport.WriteBuffer
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol


object DioApiService {


    private fun AioSession.send(message: String) {
        val writeBuffer: WriteBuffer = writeBuffer()
        val data = message.toByteArray()
        writeBuffer.write(data)
        writeBuffer.flush()
    }

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
            addHandle(this)
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
        server.setReadBufferSize(10485760 * 2) // 20m
        return server
    }


}

object MyMessageProcessor : MessageProcessor<String?> {

    private var handle = setOf<DioApiService.NativeMessageProcessing>()

    fun addHandle(processor: DioApiService.NativeMessageProcessing) {
        handle = handle.plus(processor)
    }

    override fun process(session: AioSession?, msg: String?) {
        println("接收到数据:$msg")
        if (msg != null) {
            jsonStringToModel(msg, session)
        }
    }

    private fun jsonStringToModel(msg: String, session: AioSession?) {
        try {
            val json = JSONObject.parse(msg)
            println("处理器数量:${handle.size} ${handle.map { it::class.java }}")
            handle.forEach {
                it.handleFlutterAppMessage(msg, json, session)
            }
        } catch (_: Exception) {
            handle.forEach {
                it.handleFlutterAppMessage(msg, null, session)
            }
        }
    }

    override fun stateEvent(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {
        handle.forEach {
            it.stateEvent(session, stateMachineEnum, throwable)
        }
        defaultEventHandle(session, stateMachineEnum)
        super.stateEvent(session, stateMachineEnum, throwable)
    }


    private fun defaultEventHandle(session: AioSession?, stateMachineEnum: StateMachineEnum?) {
        when (stateMachineEnum) {
            StateMachineEnum.NEW_SESSION -> {
                DioApiService.addSession(session)
            }

            StateMachineEnum.INPUT_SHUTDOWN -> DioApiService.removeSession(session)
            StateMachineEnum.SESSION_CLOSING -> DioApiService.removeSession(session)
            StateMachineEnum.SESSION_CLOSED -> DioApiService.removeSession(session)
            StateMachineEnum.REJECT_ACCEPT -> DioApiService.removeSession(session)
            StateMachineEnum.ACCEPT_EXCEPTION -> DioApiService.removeSession(session)
            else -> {}
        }
    }

}