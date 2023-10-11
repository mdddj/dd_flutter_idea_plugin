package shop.itbug.fluttercheckversionx.socket.service

import com.alibaba.fastjson2.JSONObject
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import org.smartboot.socket.transport.WriteBuffer
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol
import shop.itbug.fluttercheckversionx.window.sp.SpManager


object DioApiService {

    private fun AioSession.send(message: String) {
        val writeBuffer: WriteBuffer = writeBuffer()
        val data = message.toByteArray()
        writeBuffer.write(data)
        writeBuffer.flush()
    }

    private val sessions = mutableSetOf<AioSession>()

    fun getSessions() = sessions

    fun addSession(session: AioSession?){
        session?.let { sessions.add(it) }
    }

    fun removeSession(session: AioSession?){
        session?.let { sessions.remove(it) }
    }

    private fun sendMessage(message: String) {
        sessions.forEach {
            it.send(message)
        }
    }


    fun sendByMap(map: MutableMap<String,Any>){
        sendMessage(JSONObject.toJSONString(map))
    }


    interface HandleFlutterApiModel {
        fun handleModel(model: ProjectSocketService.SocketResponseModel)
        fun stateEvent(
            session: AioSession?,
            stateMachineEnum: StateMachineEnum?,
            throwable: Throwable?
        )

        fun covertJsonError(e: Exception, aio: AioSession?)
    }

    fun builder(port: Int, handle: HandleFlutterApiModel): AioQuickServer {
        val server = AioQuickServer(port, StringProtocol(), MyMessageProcessor(handle))
        server.setBannerEnabled(false)
        server.setReadBufferSize(10485760 * 2) // 20m
        return server
    }


}

class MyMessageProcessor(private val handle: DioApiService.HandleFlutterApiModel) : MessageProcessor<String?> {
    override fun process(session: AioSession?, msg: String?) {
        if (msg != null) {
            jsonStringToModel(msg, session)
        }
    }

    private fun jsonStringToModel(msg: String, session: AioSession?) {
        try {
            val jsonObj = JSONObject.parse(msg)
            val type = jsonObj.getString("type")
            if(type == SpManager.KEYS || type == SpManager.VALUE_GET){
                SpManager(msg).handle()
            }else{
                val responseModel = jsonObj.to(ProjectSocketService.SocketResponseModel::class.java)
                handle.handleModel(responseModel)
            }
        } catch (e: Exception) {
            handle.covertJsonError(e, session)
        }
    }

    override fun stateEvent(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {
        handle.stateEvent(session, stateMachineEnum, throwable)
        defaultEventHandle(session, stateMachineEnum)
        super.stateEvent(session, stateMachineEnum, throwable)
    }


    private fun defaultEventHandle(session: AioSession?, stateMachineEnum: StateMachineEnum?) {
        when(stateMachineEnum){
            StateMachineEnum.NEW_SESSION -> {
                DioApiService.addSession(session)
            }
            StateMachineEnum.INPUT_SHUTDOWN ->  DioApiService.removeSession(session)
            StateMachineEnum.SESSION_CLOSING -> DioApiService.removeSession(session)
            StateMachineEnum.SESSION_CLOSED -> DioApiService.removeSession(session)
            StateMachineEnum.REJECT_ACCEPT -> DioApiService.removeSession(session)
            StateMachineEnum.ACCEPT_EXCEPTION -> DioApiService.removeSession(session)
            else -> {}
        }
    }

}