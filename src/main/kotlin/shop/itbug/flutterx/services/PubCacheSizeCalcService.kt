package shop.itbug.flutterx.services

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.util.messages.Topic
import kotlinx.coroutines.*
import shop.itbug.flutterx.model.formatSize
import shop.itbug.flutterx.tools.DartPubTools
import shop.itbug.flutterx.tools.log


@Service(Service.Level.PROJECT)
class PubCacheSizeCalcService(val project: Project) : VirtualFileVisitor<VirtualFile>(), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var size: Long = 0
    private var cachePath: String? = null
    private var cacheFileName: String? = null
    private var checkJob: Job? = null
    fun getCachePath() = cachePath
    suspend fun startCheck() {
        if (size != 0L) {
            size = 0
        }
        val dartPubCacheDir = DartPubTools.getPubCacheDir()
        cachePath = dartPubCacheDir?.path
        cacheFileName = dartPubCacheDir?.name
        if (dartPubCacheDir != null) {
            try {
                VfsUtilCore.visitChildrenRecursively(dartPubCacheDir, this)
            } catch (ex: ProcessCanceledException) {
                log().warn("处理已经关闭")
                throw ex
            } catch (e: Exception) {
                log().warn("计算缓存大小失败", e)
            }
        } else {
            println("没有找到缓存目录.")
        }
    }

    //刷新
    fun refreshCheck() {
        size = 0
        if (checkJob?.isActive == true) {
            checkJob?.cancel()
        }
        ProgressManager.checkCanceled()
        checkJob = scope.launch(Dispatchers.IO) {
            try {
                startCheck()
            } catch (e: Exception) {
                log().warn("检查失败：$e")
            }
        }
    }

    override fun visitFile(file: VirtualFile): Boolean {
        size += file.length
        return true
    }

    /**
     * 在文件浏览器中打开dart缓存目录
     */
    fun openDir() {
        scope.launch(Dispatchers.EDT) {
            val file = DartPubTools.getPubCacheDir()
            if (file != null) {
                BrowserUtil.browse(file)
            }
        }
    }

    override fun afterChildrenVisited(file: VirtualFile) {
        checkJob?.ensureActive()
        if (file.name == cacheFileName) {
            project.messageBus.syncPublisher(TOPIC).calcComplete(size, formatSize(size))
        }
        super.afterChildrenVisited(file)
    }


    fun getCurrentSizeFormatString(): String = if (size > 0L) formatSize(size) else ""
    fun getPubCacheDirPathString(): String = if (cachePath != null) cachePath!! else ""

    override fun dispose() {
        scope.cancel()
    }

    companion object {
        val TOPIC = Topic.create("PubCacheSizeCalc", Listener::class.java)
        fun getInstance(project: Project) = project.service<PubCacheSizeCalcService>()
    }

    interface Listener {
        /**
         * 统计pub cache 大小完成
         */
        fun calcComplete(len: Long, formatString: String)
    }
}