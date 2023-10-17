package shop.itbug.fluttercheckversionx.activity

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import io.flutter.sdk.FlutterSdk
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.FlutterService
import shop.itbug.fluttercheckversionx.services.Release
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.RunUtil

/**
 * 梁典典
 * 当项目打开的时候,会执行这个类的runActivity方法
 * 在这里启动一个子线程去检测项目中的pubspec.yaml文件.并执行检测新版本
 */
class FlutterProjectOpenActivity : StartupActivity.Background, Disposable {


    private lateinit var connect: MessageBusConnection

    /**
     * 项目在idea中打开时执行函数
     *
     */

    private fun checkAndAutoGenFile(projectPath: String, file: VirtualFile, project: Project) {
        var filePath = file.canonicalPath
        filePath = filePath?.replace("$projectPath/", "")
        if (filePath != null) {
            if (filePath.indexOf("assets") == 0) {
                MyDartPsiElementUtil.autoGenerateAssetsDartClassFile(project, "assets", true)
            }
        }
    }

    override fun dispose() {
        connect.disconnect()
    }


    fun run(project: Project) {
        ///监听assets资源目录更改事件
        if (project.isDisposed) {
            return
        }
        connect = project.messageBus.connect(this)
        connect.subscribe(VirtualFileManager.VFS_CHANGES, object :
            BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                val projectPath = project.basePath
                val setting = GenerateAssetsClassConfig.getGenerateAssetsSetting()
                if (!setting.autoListenFileChange) {
                    return
                }
                if (projectPath != null) {
                    events.forEach {
                        it.file?.apply {
                            checkAndAutoGenFile(projectPath, this, project)
                        }
                    }
                }

                super.after(events)
            }
        })

    }


    /**
     * 检测 flutter最新版本
     */
    private fun checkFlutterLastVersion(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Detecting Flutter version...") {
            override fun run(p0: ProgressIndicator) {
                val currentFlutterVersion = FlutterSdk.getFlutterSdk(project)?.version
                currentFlutterVersion?.let {
                    val version = FlutterService.getVersion()
                    version?.apply {
                        val release = releases.find { o -> o.hash == currentRelease.stable }
                        release?.let { r ->
                            if (r.version != it.versionText) {
                                println("has new version")
                                showTip(r, project)
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * 弹出通知
     */
    fun showTip(release: Release, project: Project) {
        val createNotification =
            NotificationGroupManager.getInstance().getNotificationGroup("flutter_version_check").createNotification(
                "The new Flutter version is ready",
                "Update to the latest version: ${release.version}  (dart:${release.dartSDKVersion})",
                NotificationType.INFORMATION,
            )
        createNotification.setIcon(MyIcons.flutter)



        createNotification.addAction(
            object : AnAction("Upgrade") {
                override fun actionPerformed(p0: AnActionEvent) {
                    RunUtil.runCommand(project, "flutter upgrade", "flutter upgrade")
                    createNotification.hideBalloon()
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.icon = MyIcons.flutter
                    super.update(e)
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }

            },
        )
        createNotification.addAction(object : AnAction("Cancel") {
            override fun actionPerformed(p0: AnActionEvent) {
                createNotification.hideBalloon()

            }
        })
        createNotification.notify(project)
    }


    override fun runActivity(project: Project) {
        run(project)
        checkFlutterLastVersion(project)
    }
}