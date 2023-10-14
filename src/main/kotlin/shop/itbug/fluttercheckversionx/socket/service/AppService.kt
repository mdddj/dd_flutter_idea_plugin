package shop.itbug.fluttercheckversionx.socket.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.bus.DioWindowCleanRequests
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.listeners.FlutterProjectChangeEvent
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategoryTypeEnum
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.cache.UserRunStartService
import shop.itbug.fluttercheckversionx.services.event.UserLoginStatusEvent
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.util.CredentialUtil
import java.util.concurrent.atomic.AtomicReference

@Service
class AppService : DioApiService.HandleFlutterApiModel {


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


    //当前选中的项目
    var currentSelectName: AtomicReference<String?> = AtomicReference<String?>(null)

    val dioServerStatus: StateMachineEnum? get() = socketServerState

    //右键选中的项目
    var currentSelectRequest: Request? = null

    private val messageBus get() = ApplicationManager.getApplication().messageBus


    ///接口列表
    var requestsList = mutableListOf<Request>()

    ///添加一下接口
    fun addRequest(item: Request) {
        if (requestsList.isEmpty()) {
            changeCurrentSelectFlutterProjectName(item.projectName)
        }
        requestsList.add(item)
    }


    /**
     * 存储了flutter项目
     * 键是项目名称
     * 值是请求列表
     */
    val flutterProjects get() = requestsList.groupBy { it.projectName }


    private val userRunStartManager = Thread(UserRunStartService())
    private val chatRoomLoadManager = Thread(ChatRoomsLoadThread())
    lateinit var dioThread: Thread

    init {
        userRunStartManager.start()
        chatRoomLoadManager.start()
        note.jdbc.SqliteConnectManager
    }


    /**
     * 设置dio接口监听状态
     */
    fun setDioSocketState(isStart: Boolean) {
        socketIsInit = isStart
    }

    /**
     * dio接口监听服务是否启动
     */
    val dioIsStart get() = socketIsInit


    //更新当前选中的项目名称
    fun changeCurrentSelectFlutterProjectName(appName: String) {
        currentSelectName.updateAndGet { appName }
        ApplicationManager.getApplication().messageBus.syncPublisher(FlutterProjectChangeEvent.topic)
            .changeProject(appName)
    }

    /**
     * 获取全部的请求,不区分项目
     */
    fun getAllRequest(): List<Request> {
        return requestsList
    }


    ///获取当前选中项目的接口列表
    fun getCurrentProjectAllRequest(): List<Request> {
        currentSelectName.get()?.let {
            return flutterProjects[it] ?: emptyList()
        }
        return emptyList()
    }

    /**
     * 只支持当前选中的项目
     * 清空全部的请求
     */
    fun cleanAllRequest() {
        currentSelectName.get()?.apply {
            val result = requestsList.removeAll { it.projectName == this }
            if (result) {
                DioWindowCleanRequests.fire()
            } else {
                println("删除失败")
            }
        }
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
                call: Call<JSONResult<List<ResourceCategory>>>, response: Response<JSONResult<List<ResourceCategory>>>
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


    companion object {
        @JvmStatic
        fun getInstance() = service<AppService>()
    }

    override fun handleModel(model: ProjectSocketService.SocketResponseModel) {
    }

    override fun stateEvent(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {
    }

    override fun covertJsonError(e: Exception, aio: AioSession?) {
    }

}