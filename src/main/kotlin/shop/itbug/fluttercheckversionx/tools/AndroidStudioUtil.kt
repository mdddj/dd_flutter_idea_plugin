package shop.itbug.fluttercheckversionx.tools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import java.io.File


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
     * 是否为macos设备
     */
    val isMacos: Boolean = SystemInfo.isMac



    // 在 macOS 或 Linux 上打开指定目录
    fun openAndroidStudioWithDirectory() {
        val androidFile = File(androidPath)
        if(androidFile.exists()){
            ProjectManager.getInstance().loadAndOpenProject(androidPath)
        }else{
            showMessage("android dir not exist")
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