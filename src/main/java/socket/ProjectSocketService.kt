package socket

import cn.hutool.core.lang.Console
import cn.hutool.json.JSONUtil
import com.google.gson.Gson
import com.intellij.openapi.project.Project
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import org.smartboot.socket.transport.WriteBuffer
import java.io.IOException


// socket 连接flutter项目服务类
class ProjectSocketService {

   lateinit var server: AioQuickServer

    companion object {

    }

    /// 项目打开,开启一个socket服务,进行接口监听传输
    @OptIn(DelicateCoroutinesApi::class)
    fun onOpen(project: Project) {
        GlobalScope.launch {
            launch {

                val processor: MessageProcessor<String?> =
                    MessageProcessor<String?> { session, msg ->
                        println("receive from client: $msg")

                    }

                server = AioQuickServer(9999, StringProtocol(), processor)
                server.start()
            }
        }
    }


    /**
     * flutter端穿过来的json数据
     */
    private fun flutterClienJsonHandle(json: String){
        Console.log(json)
        try{
            val resposneModel = JSONUtil.toBean(json, SocketResponseModel::class.java)
            Console.log(resposneModel.url + "--" + resposneModel.methed)
        }catch (e: Exception){
            Console.log("解析出错了");
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
         val data: Any,
         val methed: String,
         val queryParams: Map<String, Any>,
         val url: String,
         val statusCode: Int,
         val body: Any,
         val headers: Map<String, Any>,
         val responseHeaders: Map<String, Any>
    );

}
