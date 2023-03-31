package shop.itbug.fluttercheckversionx.socket.service

import com.alibaba.fastjson2.JSONObject
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol

object DioApiService {
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
            val responseModel = JSONObject.parseObject(msg, ProjectSocketService.SocketResponseModel::class.java)
            handle.handleModel(responseModel)
        } catch (e: Exception) {
            handle.covertJsonError(e, session)
        }
    }

    override fun stateEvent(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {
        handle.stateEvent(session, stateMachineEnum, throwable)
        super.stateEvent(session, stateMachineEnum, throwable)
    }


}