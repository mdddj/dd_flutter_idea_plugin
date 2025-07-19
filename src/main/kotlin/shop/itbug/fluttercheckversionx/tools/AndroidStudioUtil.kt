package shop.itbug.fluttercheckversionx.tools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path


/**
 * 项目工具类
 */
class FlutterProjectUtil(val project: Project) {

    /**
     * 读取项目路径
     */
    private fun projectPath(dir: String): String = "${project.guessProjectDir()?.path}" + File.separator + dir

    /**
     * 项目的安卓目录路径
     */
    private val androidPath = projectPath("android")

    /**
     * 项目的ios目录路径
     */
    private val iosPath = projectPath("ios")

    /**
     * macos目录路径
     */
    private val macosPath = projectPath("macos")

    /**
     * 安卓目录是否存在
     */
    val androidDirIsExist: Boolean get() = MyFileUtil.fileIsExists(androidPath)

    /**
     * 是否有ios目录
     */
    val iosDirIsExist: Boolean get() = File(iosPath).exists()

    /**
     * 是否有macos目录
     */
    val macosDirIsExist: Boolean get() = File(macosPath).exists()


    /**
     * 项目的Android目录
     */
    private val androidCommandLineTool = GeneralCommandLine("studio")

    /**
     * macos as 默认查找路径
     */
    private val macAsDefaultFindPath = Path("/Applications/Android Studio.app")

    /**
     * win as 默认查找路径
     */
    private val windowsAsDefaultFindPath = Path("C:\\Program Files\\Android\\Android Studio\\bin\\studio64.exe")


    /**
     * 是否为macos设备
     */
    val isMacos: Boolean = SystemInfo.isMac

    /**
     * 获取默认查找路径
     */
    private val defaultFindPath: Path
        get() {
            if (SystemInfo.isMac) {
                return macAsDefaultFindPath
            } else if (SystemInfo.isWindows) {
                return windowsAsDefaultFindPath
            }
            throw UnsupportedOperationException("Not implemented")
        }

    /**
     * 检查 Android Studio 是否安装
     */
    val isAndroidStudioInstalled: Boolean get() = MyFileUtil.pathIsExists(defaultFindPath)

    // 在 macOS 或 Linux 上打开指定目录
    fun openAndroidStudioWithDirectory() {
        if (SystemInfo.isMac) {//macos
            val openProjectCommandArgs =
                arrayOf(androidPath)
            androidCommandLineTool.withParameters(*openProjectCommandArgs)
            try {
                ExecUtil.execAndReadLine(androidCommandLineTool)
            } catch (e: Exception) {
                openFailed(e.localizedMessage)
            }
        } else if (SystemInfo.isWindows) {//macos
            val openProjectCommandArgs =
                arrayOf(androidPath)
            androidCommandLineTool.withParameters(*openProjectCommandArgs)
            try {
                ExecUtil.execAndReadLine(androidCommandLineTool)
            } catch (e: Exception) {
                openFailed(e.localizedMessage)
            }
        }
    }

    /**
     * 在xcode中打开ios目录
     */
    fun openIosInXCode() {
        runExd(iosPath)
    }


    /**
     * 在xcode中打开macos
     */
    fun openMacosInXCode() {
        runExd(macosPath)
    }

    private fun runExd(filepath: String) {
        log().warn("打开iOS项目,xcode $iosPath")
        val oc = GeneralCommandLine("xed")
        oc.workDirectory = File(filepath)

        val args = arrayOf(".")
        oc.withParameters(*args)
        try {
            ExecUtil.execAndReadLine(oc)
        } catch (e: Exception) {
            openFailed(e.localizedMessage)
        }
    }

    private fun openFailed(msg: String) {
        log().warn("执行命令失败:$msg")
        showMessage("Open failed:${msg}")
    }

    private fun showMessage(msg: String) {
        val id = "open_flutter_project_in_ide"
        val notification = NotificationGroupManager.getInstance().getNotificationGroup(id).createNotification(
            msg, NotificationType.ERROR,
        )
        NotificationsManager.getNotificationsManager().showNotification(
            notification, project
        )
    }

}