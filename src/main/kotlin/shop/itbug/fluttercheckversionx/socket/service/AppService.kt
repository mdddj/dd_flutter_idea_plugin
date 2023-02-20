package shop.itbug.fluttercheckversionx.socket.service

import cn.hutool.core.lang.Console
import com.alibaba.fastjson2.JSONObject
import com.google.common.collect.ImmutableSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.bus.ProjectListChangeBus
import shop.itbug.fluttercheckversionx.bus.SocketConnectStatusMessageBus
import shop.itbug.fluttercheckversionx.bus.SocketMessageBus
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategoryTypeEnum
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.cache.UserRunStartService
import shop.itbug.fluttercheckversionx.services.event.UserLoginStatusEvent
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.socket.StringProtocol
import shop.itbug.fluttercheckversionx.util.CredentialUtil
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

class AppService {


    lateinit var project: Project

    // 全局的socket监听服务
    private lateinit var server: AioQuickServer

    //用户信息
    var user: User? = null

    //聊天房间列表
    var chatRooms: List<ResourceCategory> = emptyList()

    //当前选中的聊天房间
    var currentChatRoom: ResourceCategory? = null

    //socket服务是否正常启动
    private var socketIsInit = false

    //socket服务状态
    private var socketServerState: StateMachineEnum? = null

    //项目名称列表
    var projectNames: List<String> = emptyList()

    //监听列表
    private var listenings = AtomicReference<ImmutableSet<Runnable>>(ImmutableSet.of())

    //当前选中的项目
    var currentSelectName: AtomicReference<String?> = AtomicReference<String?>(null)
    //当前选中的方法
    var currentSelectMethodType: AtomicReference<String?> = AtomicReference(null)

    val dioServerStatus: StateMachineEnum? get() = socketServerState

    //自动滚动到底部
     var apiListAutoScrollerToMax = true

    /**
     * 存储了flutter项目
     * 键是项目名称
     * 值是请求列表
     */
    private var flutterProjects = mutableMapOf<String, List<ProjectSocketService.SocketResponseModel>>()


    private val userRunStartManager = Thread(UserRunStartService())
    private val chatRoomLoadManager = Thread(ChatRoomsLoadThread())
    lateinit var dioThread: Thread

    init {
        userRunStartManager.start()
        chatRoomLoadManager.start()
        ChatSocketService.connect()
        note.jdbc.SqliteConnectManager
    }

    /**
     * 如果监听到api接口请求,是否自动滚动到最底部
     */
    fun setIsAutoScrollToMax(value: Boolean) {
        apiListAutoScrollerToMax = value
    }


    /**
     * 初始化socket服务,并处理flutter端传输过来的值
     */
    fun initSocketService(p: Project) {
        if (socketIsInit) return
        project = p
        val port = PluginStateService.appSetting.serverPort
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
                socketServerState = stateMachineEnum
                messageBus.syncPublisher(SocketConnectStatusMessageBus.CHANGE_ACTION_TOPIC)
                    .statusChange(aioSession = session, stateMachineEnum = stateMachineEnum)
                when (stateMachineEnum) {
                    StateMachineEnum.NEW_SESSION -> {
//                        newSessionHandle()
                        println("新的链接.....")
                    }

                    StateMachineEnum.SESSION_CLOSED -> {
                        MyNotificationUtil.toolWindowShowMessage(
                            project,
                            "典典:dio监听模块意外断开,请重新连接",
                        )
                        println("aio已断开: $throwable")
                    }

                    else -> {
                        println("aio意外断开: $throwable")
                    }
                }
            }
        })
        server.setBannerEnabled(false)
        server.setReadBufferSize(10485760 * 2) // 20m
        val appSocketThread = AppSocketThread(server, project) {
            socketIsInit = it
        }
        dioThread = Thread(appSocketThread)
        dioThread.start()
    }


    //添加监听
    fun addListening(runnable: Runnable) {
        listenings.updateAndGet { old ->
            val toMutableList = old.toMutableList()
            toMutableList.add(runnable)
            ImmutableSet.copyOf(toMutableList)
        }
    }

    //移除监听
    fun removeListening(runnable: Runnable) {
        listenings.updateAndGet { old ->
            val toMutableList = old.toMutableList()
            toMutableList.remove(runnable)
            ImmutableSet.copyOf(toMutableList)
        }
    }

    //通知更新
    private fun fireChangeToListening() {
        SwingUtilities.invokeLater {
            for (runnable in listenings.get()) {
                try {
                    runnable.run()
                } catch (e: Exception) {
                    println("警告: 更新失败:$e")
                }
            }
        }
    }


    //更新当前选中的项目名称
    fun changeCurrentSelectFlutterProjectName(appName: String) {
        currentSelectName.updateAndGet { appName }
        fireChangeToListening()
    }

    /**
     * 更新过滤类型
     */
    fun changeCurrentSelectFilterMethodType(type: String) {
        currentSelectMethodType.updateAndGet { type }
        fireChangeToListening()
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
            responseModel.projectName?.apply {
                if(!flutterProjects.keys.contains(this)){
                    val old = mutableListOf<String>()
                    flutterProjects.keys.forEach {
                        old.add(it)
                    }
                    old.add(this)
                    fireFlutterNamesChangeBus(old.toList())
                }

                flutterProjects[this] = reqsAdded
            }
            projectNames = flutterProjects.keys.toList()
            SocketMessageBus.fire(responseModel)
        } catch (e: Exception) {
            Console.log("解析出错了:$e")
        }
    }


    private fun fireFlutterNamesChangeBus(list: List<String>) {
            ProjectListChangeBus.fire(list)
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


    fun getCurrentProjectAllRequest() : List<Request> {
        if(currentSelectName.get()!=null){
            return flutterProjects[currentSelectName.get()!!]?.toList() ?: emptyList()
        }
        return emptyList()
    }

    /**
     * 清空全部的请求
     */
    fun cleanAllRequest() {
        flutterProjects.clear()
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
                        user = body.data
                        messageBus.syncPublisher(UserLoginStatusEvent.TOPIC).loginSuccess(user)
                    } else {
                        CredentialUtil.removeToken()
                    }
                }

                override fun onFailure(call: Call<JSONResult<User?>>, t: Throwable) {
                    MyNotificationUtil.toolWindowShowMessage(project, "登录失败,${t}", MessageType.ERROR)
                    t.printStackTrace()
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