package shop.itbug.fluttercheckversionx.socket.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.smartboot.socket.StateMachineEnum
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.bus.DioWindowCleanRequests
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.listeners.FlutterProjectChangeEvent
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import java.util.concurrent.atomic.AtomicReference

@Service
class AppService : DioApiService.HandleFlutterApiModel {


    //socket服务是否正常启动
    private var socketIsInit = false

    //项目名称列表
    var projectNames: List<String> = emptyList()


    //当前选中的项目
    var currentSelectName: AtomicReference<String?> = AtomicReference<String?>(null)


    ///接口列表
    private var requestsList = mutableListOf<Request>()

    ///添加一下接口
    private fun addRequest(item: Request) {
        if (requestsList.isEmpty()) {
            changeCurrentSelectFlutterProjectName(item.projectName, null)
        }
        requestsList.add(item)
    }


    /**
     * 存储了flutter项目
     * 键是项目名称
     * 值是请求列表
     */
    val flutterProjects get() = requestsList.groupBy { it.projectName }


    init {
        register()
    }


    /**
     * 添加测试api接口
     */
    private fun addTestRequestItem() {
        addRequest(ProjectSocketService.getTestApi())
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
    fun changeCurrentSelectFlutterProjectName(appName: String, project: Project?) {
        currentSelectName.updateAndGet { appName }
        ApplicationManager.getApplication().messageBus.syncPublisher(FlutterProjectChangeEvent.topic)
            .changeProject(appName, project)
    }

    //当前选中项目
    fun getCurrentSelectProjectName() = currentSelectName.get()

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

    ///刷新接口列表
    fun refreshProjectRequest(project: Project) {
        currentSelectName.get()?.let {
            changeCurrentSelectFlutterProjectName(it, project)
        }
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

    companion object {
        fun getInstance() = service<AppService>()
    }

    override fun handleModel(model: ProjectSocketService.SocketResponseModel) {
        addRequest(model)
    }

    override fun stateEvent(session: AioSession?, stateMachineEnum: StateMachineEnum?, throwable: Throwable?) {
    }

    override fun covertJsonError(e: Exception, aio: AioSession?) {
    }

}