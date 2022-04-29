package shop.itbug.fluttercheckversionx.socket.service

import cn.hutool.core.lang.Console
import com.google.gson.Gson
import com.intellij.openapi.application.ApplicationManager
import shop.itbug.fluttercheckversionx.form.socket.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.transport.AioQuickServer
import shop.itbug.fluttercheckversionx.services.SocketMessageBus
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol

class AppService {


    /**
     * socket服务,在携程中运行,避免阻塞UI
     */
    private val scope = CoroutineScope(Dispatchers.IO)


    /**
     * 全局的socket监听服务
     */
    private var server: AioQuickServer? = null

    /**
     * 存储了flutter项目
     *
     * 键是项目名称
     * 值是请求列表
     */
    private var flutterProjects = mutableMapOf<String,List<ProjectSocketService.SocketResponseModel>>()


    /**
     * 初始化socket服务,并处理flutter端传输过来的值
     */
    fun initSocketService() {
        if (server == null) {
            scope.launch {
                val processor: MessageProcessor<String?> =
                    MessageProcessor<String?> { _, msg ->
                        flutterClienJsonHandle(msg)
                    }
                server = AioQuickServer(9999, StringProtocol(), processor)
                server!!.setReadBufferSize(10485760) // 10m
                try {
                    server!!.start()
                }catch (e: Exception){
                    println("Socket服务启动失败,原因是:${e.localizedMessage}")
                }
            }
        }
    }


    /**
     * flutter端穿过来的json数据
     * 对齐进一步处理
     * 通过idea的开发消息总线进行传输到UI工具窗口对用户进行展示内容
     */
    private fun flutterClienJsonHandle(json: String) {
        try {
            val responseModel = Gson().fromJson(json, ProjectSocketService.SocketResponseModel::class.java)
            val reqs = flutterProjects[responseModel.projectName] ?: emptyList()
            val reqsAdded = reqs.plus(responseModel)
            flutterProjects[responseModel.projectName] = reqsAdded
            ApplicationManager.getApplication().messageBus.syncPublisher(SocketMessageBus.CHANGE_ACTION_TOPIC)
                .handleData(responseModel)
        } catch (e: Exception) {
            Console.log("解析出错了:$e")
        }
    }


    /**
     * 获取全部的请求,不区分项目
     */
    fun getAllRequest(): List<Request> {
        val all = mutableListOf<Request>()
         flutterProjects.values.forEach {
             all.addAll(it)
         }
        return all
    }

    /**
     * 清空全部的请求
     */
    fun cleanAllRequest(){
        flutterProjects.clear()
    }


    /**
     * 获取全部的项目名
     */
    fun getAllProjectNames(): ArrayList<String> {
        return ArrayList(flutterProjects.keys)
    }

}