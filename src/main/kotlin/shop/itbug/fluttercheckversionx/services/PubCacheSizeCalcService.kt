package shop.itbug.fluttercheckversionx.services

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.util.messages.Topic
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.socket.formatSize
import shop.itbug.fluttercheckversionx.tools.DartPubTools


class PubCacheSizeCalcPostStart : ProjectActivity {
    override suspend fun execute(project: Project) {
        PubCacheSizeCalcService.getInstance(project).startCheck()
    }
}

@Service(Service.Level.PROJECT)
class PubCacheSizeCalcService(val project: Project) : VirtualFileVisitor<VirtualFile>(), Disposable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var size: Long = 0
    private var cachePath: String? = null
    suspend fun startCheck() {
        val dartPubCacheDir = DartPubTools.getPubCacheDir()
        cachePath = dartPubCacheDir?.path
        if (dartPubCacheDir != null) {
            println("缓存目录cache dir: ${dartPubCacheDir.path}")
            VfsUtilCore.visitChildrenRecursively(dartPubCacheDir, this)
        } else {
            println("没有找到缓存目录.")
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
        project.messageBus.syncPublisher(TOPIC).calcComplete(size, formatSize(size))
        super.afterChildrenVisited(file)
    }

    fun getCurrentSizeFormatString(): String = if (size > 0L) formatSize(size) else ""
    fun getPubCacheDirPathString(): String = if (cachePath != null) cachePath!! else ""

    override fun dispose() {
        println("PubCacheSizeCalcService -- dispose")
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