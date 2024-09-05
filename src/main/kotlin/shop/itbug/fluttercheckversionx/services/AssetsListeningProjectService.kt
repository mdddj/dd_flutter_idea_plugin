package shop.itbug.fluttercheckversionx.services

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.actions.components.MyButtonAnAction
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.tools.FlutterVersionTool
import shop.itbug.fluttercheckversionx.tools.MyFlutterVersion
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.Util

class MyAssetGenPostStart : ProjectActivity {
    override suspend fun execute(project: Project) {
        AssetsListeningProjectService.getInstance(project).initListening()
    }
}

class MyProjectListening : ProjectManagerListener {

    override fun projectClosing(project: Project) {
        AssetsListeningProjectService.getInstance(project).dispose()
        super.projectClosing(project)
    }
}

@Service(Service.Level.PROJECT)
class AssetsListeningProjectService(val project: Project) : Disposable {
    private lateinit var connect: MessageBusConnection
    private var checkFlutterVersionTask: CheckFlutterVersionTask = CheckFlutterVersionTask()

    companion object {
        fun getInstance(project: Project): AssetsListeningProjectService {
            return project.getService(AssetsListeningProjectService::class.java)
        }
    }

    override fun dispose() {
        println("generate dispose.....")
        checkFlutterVersionTask.onCancel()
        if (DioListingUiConfig.setting.checkFlutterVersion) {
            checkFlutterVersionTask.onCancel()
        }
        connect.dispose()
    }

    ///初始化
    fun initListening() {
        if (DioListingUiConfig.setting.checkFlutterVersion) {
            ProgressManager.getInstance().run(checkFlutterVersionTask)
        }
// 通知测试
//        checkFlutterVersionTask.showTestTip(project)
        checkAssetsChange()
    }


    private fun checkAssetsChange() {
        connect = project.messageBus.connect()
        connect.subscribe(VirtualFileManager.VFS_CHANGES, object :
            BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                super.after(events)
                if (project.isDisposed) {
                    return
                }
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
            }
        })
    }

    private fun checkAndAutoGenFile(projectPath: String, file: VirtualFile, project: Project) {
        var filePath = file.canonicalPath
        filePath = filePath?.replace("$projectPath/", "")
        if (filePath != null) {
            if (filePath.indexOf("assets") == 0) {
                MyDartPsiElementUtil.autoGenerateAssetsDartClassFile(project, "assets", true)
            }
        }
    }

    ///检测flutter新版本弹出
    private inner class CheckFlutterVersionTask : Task.Backgroundable(project, "Detecting Flutter version...") {
        override fun run(indicator: ProgressIndicator) {
            val flutterChannel = Util.getFlutterChannel()
            val currentFlutterVersion = runBlocking { FlutterVersionTool.readVersionFromSdkHome(project) }
            if (flutterChannel == null) {
                return
            }
            currentFlutterVersion?.let { c ->
                val version = FlutterService.getVersion()
                version.apply {
                    val hash = version.getCurrentReleaseByChannel(flutterChannel)
                    val release = releases.find { o -> o.hash == hash }
                    release?.let { r ->
                        if (r.version != c.version) {
                            println("has new version")
                            showTip(r, project, currentFlutterVersion, flutterChannel)
                        }
                    }
                }
            }
        }
//
//        fun showTestTip(project: Project) {
//            println("show test tips")
//            showTip(testRelease, project, MyFlutterVersion("3.22.0"), "stable")
//        }

        /**
         * 弹出通知
         */
        fun showTip(release: Release, project: Project, currentVersion: MyFlutterVersion, channel: String) {
            val createNotification =
                NotificationGroupManager.getInstance().getNotificationGroup("flutter_version_check").createNotification(
                    "The new Flutter version is ready",
                    """
                    Update to the latest version: ${release.version}  (dart:${release.dartSDKVersion})
                    <br/>
                    <br/>
                    Current version: ${currentVersion.version}
                    Channel:$channel
                    """.trimIndent(),
                    NotificationType.INFORMATION,
                )
            createNotification.setIcon(MyIcons.flutter)


            ///执行命令
            createNotification.addAction(
                object : MyButtonAnAction("Flutter Upgrade") {
                    override fun actionPerformed(p0: AnActionEvent) {
                        RunUtil.runCommand(project, "flutter upgrade", "flutter upgrade")
                        createNotification.hideBalloon()
                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.icon = MyIcons.flutter
                        super.update(e)
                    }

                },
            )

            // --force
            createNotification.addAction(object : DumbAwareAction("Flutter upgrade --force") {
                override fun actionPerformed(e: AnActionEvent) {
                    RunUtil.runCommand(project, "flutter upgrade --force", "flutter upgrade --force")
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }
            })

            ///查看更新日志
            createNotification.addAction(object : DumbAwareAction("What's New") {
                override fun actionPerformed(e: AnActionEvent) {
                    val version = release.version.replace(".", "")
                    BrowserUtil.browse("https://github.com/flutter/flutter/blob/master/CHANGELOG.md#$version")
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }
            })

            ///关闭
            createNotification.addAction(object : DumbAwareAction("Cancel") {
                override fun actionPerformed(p0: AnActionEvent) {
                    createNotification.hideBalloon()
                    createNotification.expire()
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }
            })
            createNotification.setSuggestionType(true)
            createNotification.notify(project)
        }
    }

}