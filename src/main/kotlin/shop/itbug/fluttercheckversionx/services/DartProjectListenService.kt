package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService

/**
 * todo flutter 项目启动监听
 */

@Service(Service.Level.PROJECT)
class DartProjectListenService(val project: Project) : FlutterXVMService.Listener, Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    init {
        FlutterXVMService.getInstance(project).addListener(this)
    }


    override fun dispose() {
        scope.cancel()
    }


    companion object {
        fun getInstance(project: Project): DartProjectListenService {
            return project.getService(DartProjectListenService::class.java)
        }
    }
}