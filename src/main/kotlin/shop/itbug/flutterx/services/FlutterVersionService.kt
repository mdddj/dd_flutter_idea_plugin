package shop.itbug.flutterx.services

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import shop.itbug.flutterx.config.DioListingUiConfig
import shop.itbug.flutterx.model.FlutterLocalVersion
import shop.itbug.flutterx.tools.FlutterVersionTool
import kotlin.coroutines.CoroutineContext

@Service(Service.Level.PROJECT)
class FlutterVersionService(val project: Project) : Disposable, CoroutineScope {

    private val job = SupervisorJob()
    private var flutterVersion: FlutterLocalVersion? = null

    suspend fun getFlutterVersion(): FlutterLocalVersion? {
        if (flutterVersion == null) {
            flutterVersion = FlutterVersionTool.getLocalFlutterVersion(project)
        }
        return flutterVersion
    }

    suspend fun refreshAndGetFlutterVersion(): FlutterLocalVersion? {
        flutterVersion = null
        return getFlutterVersion()
    }

    //获取远程flutter最新版本号
    fun getRemoteFlutterVersion(): FlutterVersions? {
        try {
            val url = DioListingUiConfig.setting.checkFlutterVersionUrl
            val get: String = HttpRequests.request(url).readString()
            return Gson().fromJson(get, FlutterVersions::class.java)
        } catch (_: Exception) {
            return null
        }
    }

    //获取 flutter安装目录
    fun getFlutterHome(): VirtualFile? = FlutterVersionTool.getFlutterHome(project)

    override fun dispose() {
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    companion object {
        fun getInstance(project: Project): FlutterVersionService {
            return project.getService(FlutterVersionService::class.java)
        }
    }
}