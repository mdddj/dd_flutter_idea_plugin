package socket

import cn.hutool.core.lang.Console
import cn.hutool.json.JSONUtil
import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusFactory
import kotlinx.coroutines.*
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.transport.AioQuickServer
import services.SokcetMessageBus
import java.io.IOException


// socket 连接flutter项目服务类
class ProjectSocketService : Disposable {

   lateinit var server: AioQuickServer


   /// 请求列表
   private val flutterRequests = mutableListOf<SocketResponseModel>()

    private val scope = CoroutineScope (Dispatchers.IO)


    fun getRequests(): List<SocketResponseModel> {
        return flutterRequests
    }


    /**
     * 清空全部
     */
    fun clean(){
        flutterRequests.clear();
    }

    /// 项目打开,开启一个socket服务,进行接口监听传输
    fun onOpen(project: Project) {

        Disposer.register(project,this)

        scope.launch{
            val processor: MessageProcessor<String?> =
                MessageProcessor<String?> { _, msg ->
                    flutterClienJsonHandle(msg,project)
                }

            server = AioQuickServer(9999, StringProtocol(), processor)
            server.setReadBufferSize(10485760) // 10m
            server.start()
        }
    }


    /**
     * flutter端穿过来的json数据
     */
    private fun flutterClienJsonHandle(json: String,project: Project){
        try{
          val responseModel =   Gson().fromJson(json,SocketResponseModel::class.java)
            flutterRequests.add(responseModel)
            project.messageBus.syncPublisher(SokcetMessageBus.CHANGE_ACTION_TOPIC).handleData(responseModel)
        }catch (e: Exception){
            Console.log("解析出错了:$e");
        }
    }

    /**
     * 连接成功
     */
    private fun connected() {
        val socketDataModel = SocketDataModel("connected", "欢迎使用Idea Flutter Http监听请求插件. 服务已经正常启动. 建议反馈QQ群667186542")
        val json = Gson().toJson(socketDataModel).toString()
        pushData2Flutter(json)
    }

    @Throws(IOException::class)
    fun pushData2Flutter(response: String) {
//        channel?.write(BufferUtil.createUtf8(response))
    }

    /**
     * 发送给flutter端的模型
     */
    data class SocketDataModel(private val type: String, private val json: String)

    /**
     * 解析flutter发送过来的模型
     */
    data class SocketResponseModel(
         val data: Any?,
         val methed: String,
         val queryParams: Map<String, Any>,
         val url: String,
         val statusCode: Int,
         val body: Any,
         val headers: Map<String, Any>,
         val responseHeaders: Map<String, Any>,
         var timesatamp: Int
    )

    override fun dispose() {
        scope.cancel()
    };

}
