package shop.itbug.fluttercheckversionx.services

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.actions.components.MyButtonAnAction
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.tools.FlutterVersionTool
import shop.itbug.fluttercheckversionx.util.DateUtils
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.Util

//
class MyAssetGenPostStart : ProjectActivity {
    val logger = thisLogger()
    override suspend fun execute(project: Project) {
        AssetsListeningProjectService.getInstance(project).initListening()
        FlutterL10nService.getInstance(project).checkAllKeys()
        DumbService.getInstance(project).runWhenSmart {
            val setting = PluginConfig.getInstance(project)
            if (!project.isDisposed && setting.state.scanDartStringInStart) {
                FlutterL10nService.getInstance(project).startScanStringElements()
            }
        }


        FlutterXVMService.getInstance(project)
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
    private val logger = thisLogger()
    private val connect: MessageBusConnection = project.messageBus.connect(this)
    private var checkFlutterVersionTask: CheckFlutterVersionTask = CheckFlutterVersionTask()

    companion object {
        fun getInstance(project: Project): AssetsListeningProjectService {
            return project.getService(AssetsListeningProjectService::class.java)
        }
    }

    override fun dispose() {
        if (DioListingUiConfig.setting.checkFlutterVersion) {
            if (checkFlutterVersionTask.indication?.isRunning == true) {
                checkFlutterVersionTask.indication?.cancel()
            }
        }
    }

    ///初始化
    fun initListening() {
        if (DioListingUiConfig.setting.checkFlutterVersion) {
            ProgressManager.getInstance().run(checkFlutterVersionTask)
        }
        checkAssetsChange()
    }


    private fun checkAssetsChange() {
        connect.subscribe(VirtualFileManager.VFS_CHANGES, object :
            BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                super.after(events)
                if (project.isDisposed) {
                    return
                }
                val projectPath = project.basePath
                val setting = GenerateAssetsClassConfig.getGenerateAssetsSetting(project)
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

    private fun checkAndAutoGenFile(
        projectPath: String,
        file: VirtualFile,
        project: Project
    ) {
        var filePath = file.canonicalPath
        filePath = filePath?.replace("$projectPath/", "")
        if (filePath != null) {
            val appSetting = PluginStateService.appSetting
            if (filePath.indexOf(appSetting.assetScanFolderName) == 0) {
                MyDartPsiElementUtil.autoGenerateAssetsDartClassFile(project, appSetting.assetScanFolderName, true)
            }
        }
    }

    ///检测flutter新版本弹出
    private inner class CheckFlutterVersionTask() :
        Task.Backgroundable(project, "Detecting Flutter version...") {
        var indication: ProgressIndicator? = null
        override fun run(indicator: ProgressIndicator) {
            this.indication = indicator
            val flutterChannel = Util.getFlutterChannel()
            val currentFlutterVersion = runBlocking { FlutterVersionTool.readVersionFromSdkHome(project) }
            if (flutterChannel == null) {
                return
            }
            currentFlutterVersion?.let { c ->
                try {
                    val version = FlutterService.getVersion()
                    version.apply {
                        val hash = version.getCurrentReleaseByChannel(flutterChannel)
                        val release = releases.find { o -> o.hash == hash }
                        release?.let { r ->
                            if (r.version != c.version) {
                                showTip(r, project)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("检查 flutter 版本失败:${e.localizedMessage}")
                }
            }
        }

        /**
         * 弹出通知
         */
        fun showTip(release: Release, project: Project) {
            val html = HtmlChunk.div().addText("Flutter ${PluginBundle.get("flutter_has_new_version")}")
                .children(
                    HtmlChunk.nbsp(2),
                    HtmlChunk
                        .tag("strong")
                        .bold()
                        .addText(release.version),
                    HtmlChunk.div().children(
                        HtmlChunk.span().addText(PluginBundle.get("flutter_has_new_version_date")),
                        HtmlChunk.nbsp(2),
                        HtmlChunk.span().addText(
                            DateUtils.timeAgo(
                                DateUtils.parseDate(release.releaseDate)
                            )
                        ).bold()
                    )
                )
                .toString()
            val createNotification =
                NotificationGroupManager.getInstance().getNotificationGroup("flutter_version_check").createNotification(
                    html,
                    NotificationType.INFORMATION,
                )
            createNotification.icon = MyIcons.flutter
            createNotification.setTitle("FlutterX")

            ///执行命令
            createNotification.addAction(
                object : MyButtonAnAction("Flutter Upgrade") {
                    override fun actionPerformed(p0: AnActionEvent) {
                        RunUtil.runCommand(project, "flutter upgrade", "flutter upgrade")
                        createNotification.hideBalloon()
                        createNotification.expire()
                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.icon = MyIcons.flutter
                        super.update(e)
                    }

                },
            )

            createNotification.addAction(NotShowFlutterCheckVersionAction {
                createNotification.hideBalloon()
                createNotification.expire()
            })

            ///查看更新日志
            createNotification.addAction(object : DumbAwareAction("What's New") {
                override fun actionPerformed(e: AnActionEvent) {
                    val version = release.version.replace(".", "")
                    BrowserUtil.browse("https://github.com/flutter/flutter/blob/${release.version}/CHANGELOG.md#$version")
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
            createNotification.isSuggestionType = true
            createNotification.notify(project)
        }
    }
}

//不检测新版本操作
private class NotShowFlutterCheckVersionAction(val onActioned: () -> Unit) :
    DumbAwareAction(PluginBundle.get("ig.version.check")) {
    override fun actionPerformed(p0: AnActionEvent) {
        p0.project?.let {
            DioListingUiConfig.changeSetting { setting -> setting.copy(checkFlutterVersion = false) }
            onActioned.invoke()
        }
    }
}