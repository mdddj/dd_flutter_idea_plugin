package shop.itbug.fluttercheckversionx.socket.service

import cn.hutool.core.lang.Console
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.model.example.ResourceModel
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategoryTypeEnum
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.cache.UserRunStartService
import shop.itbug.fluttercheckversionx.services.event.SocketConnectStatusMessageBus
import shop.itbug.fluttercheckversionx.services.event.SocketMessageBus
import shop.itbug.fluttercheckversionx.services.event.UserLoginStatusEvent
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol
import shop.itbug.fluttercheckversionx.util.CredentialUtil
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil

class AppService {


    lateinit var project: Project

    // 全局的socket监听服务
    private lateinit var server: AioQuickServer

    //组件示例
    var examples = emptyList<ResourceModel>()

    //用户信息
    var user: User? = null

    //聊天房间列表
    var chatRooms: List<ResourceCategory> = emptyList()

    //当前选中的聊天房间
    var currentChatRoom: ResourceCategory? = null

    //socket服务是否正常启动
    private var socketIsInit = false

    /**
     * 存储了flutter项目
     * 键是项目名称
     * 值是请求列表
     */
    private var flutterProjects = mutableMapOf<String, List<ProjectSocketService.SocketResponseModel>>()


    private val userRunStartManager = Thread(UserRunStartService())
    private val chatRoomLoadManager = Thread(ChatRoomsLoadThread())


    init {
        userRunStartManager.start()
        chatRoomLoadManager.start()
        ChatSocketService.connect()
    }

    /**
     * 初始化socket服务,并处理flutter端传输过来的值
     */
    fun initSocketService(p: Project) {
        if (socketIsInit) return
        project = p
        val port = PluginStateService.getInstance().state?.serverPort ?: "9999"
        server = AioQuickServer(port.toInt(), StringProtocol(), object : MessageProcessor<String?> {
            override fun process(session: AioSession?, msg: String?) {
                msg?.let { flutterClientJsonHandle(msg) }
            }

            override fun stateEvent(
                session: AioSession?,
                stateMachineEnum: StateMachineEnum?,
                throwable: Throwable?
            ) {
                super.stateEvent(session, stateMachineEnum, throwable)
                messageBus.syncPublisher(SocketConnectStatusMessageBus.CHANGE_ACTION_TOPIC)
                    .statusChange(aioSession = session, stateMachineEnum = stateMachineEnum)
                when (stateMachineEnum) {
                    StateMachineEnum.NEW_SESSION -> {
                        newSessionHandle()
                    }

                    StateMachineEnum.SESSION_CLOSED -> {
                        MyNotificationUtil.toolWindowShowMessage(
                            project,
                            "典典:dio监听模块意外断开,请重新连接",
                        )
                    }

                    else -> {}
                }
            }
        })
        server.setReadBufferSize(10485760) // 10m
        val thread = AppSocketThread(server, project) {
            socketIsInit = it
        }
        Thread(thread).start()
    }

    /**
     * 当有新连接进来的时候处理函数
     */
    private fun newSessionHandle() {
        MyNotificationUtil.toolWindowShowMessage(project, "FlutterCheckX Connection succeeded")
    }


    private val messageBus get() = ApplicationManager.getApplication().messageBus

    /**
     * flutter端穿过来的json数据
     * 对齐进一步处理
     * 通过idea的开发消息总线进行传输到UI工具窗口对用户进行展示内容
     */
    private fun flutterClientJsonHandle(json: String) {
        try {
            val responseModel = JSONObject.parseObject(json, ProjectSocketService.SocketResponseModel::class.java)
            val reqs = flutterProjects[responseModel.projectName] ?: emptyList()
            val reqsAdded = reqs.plus(responseModel)
            flutterProjects[responseModel.projectName] = reqsAdded
            messageBus.syncPublisher(SocketMessageBus.CHANGE_ACTION_TOPIC)
                .handleData(responseModel)
        } catch (e: Exception) {
            Console.log("解析出错了:$e")

        }
    }

    fun getRequestsWithProjectName(projectName: String): List<Request> {
        val d = flutterProjects.filter { it.key == projectName }
        if (d.isNotEmpty()) {
            return d.getValue(projectName)
        }
        return emptyList()
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
    fun cleanAllRequest() {
        flutterProjects.clear()
        messageBus.syncPublisher(SocketMessageBus.CHANGE_ACTION_TOPIC)
            .handleData(null)
    }


    /**
     * 获取全部的项目名
     */
    fun getAllProjectNames(): ArrayList<String> {
        return ArrayList(flutterProjects.keys)
    }

    /**
     * 执行登录
     */
    fun login() {
        CredentialUtil.token?.let {
            val userToken = it
            val r = SERVICE.create<ItbugService>().getUserInfo(userToken)
            r.enqueue(object : Callback<JSONResult<User?>> {
                override fun onResponse(call: Call<JSONResult<User?>>, response: Response<JSONResult<User?>>) {
                    val body = response.body()
                    if (body?.state == 200) {
                        println("登录成功:${JSONObject.toJSONString(body.data)}")
                        user = body.data
                        messageBus.syncPublisher(UserLoginStatusEvent.TOPIC).loginSuccess(user)
                    } else {
                        CredentialUtil.removeToken()
                    }
                }

                override fun onFailure(call: Call<JSONResult<User?>>, t: Throwable) {
                    CredentialUtil.removeToken()
                }
            })
        }
    }

    /**
     * 加载房间列表
     */
    fun loadRooms() {
        val call = SERVICE.create<ItbugService>().getResourceCategorys(ResourceCategoryTypeEnum.chatRoom.type)
        call.enqueue(object : Callback<JSONResult<List<ResourceCategory>>> {
            override fun onResponse(
                call: Call<JSONResult<List<ResourceCategory>>>,
                response: Response<JSONResult<List<ResourceCategory>>>
            ) {
                response.body()?.apply {
                    if (state == 200) {
                        chatRooms = data
                    }
                }
            }

            override fun onFailure(call: Call<JSONResult<List<ResourceCategory>>>, t: Throwable) {
            }

        })

    }

}