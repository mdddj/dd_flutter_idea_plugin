package shop.itbug.flutterx.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessTerminatedListener
import shop.itbug.flutterx.common.MyAction
import shop.itbug.flutterx.dialog.BatchPublishChildPackagesDialog
import shop.itbug.flutterx.dialog.BatchPublishPackageRequest
import shop.itbug.flutterx.dialog.CommandOutputDialog
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import com.intellij.platform.util.progress.reportRawProgress
import kotlinx.coroutines.ensureActive
import shop.itbug.flutterx.util.PubPackagePublishUtil
import shop.itbug.flutterx.util.launchBackgroundProgress
import shop.itbug.flutterx.util.toastWithError
import java.util.concurrent.CancellationException

class BatchPublishChildPackagesAction : MyAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val rootDirectory = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val allPackages = PubPackagePublishUtil.collectChildPackages(project, rootDirectory)
        val publishablePackages = PubPackagePublishUtil.sortForPublish(allPackages.filter { it.publishable })
        if (publishablePackages.isEmpty()) {
            project.toastWithError(PluginBundle.get("batch_publish_child_packages_no_publishable"))
            return
        }

        val dialog = BatchPublishChildPackagesDialog(
            currentProject = project,
            rootDirectory = rootDirectory,
            packages = publishablePackages,
            skippedPackages = allPackages.count { !it.publishable }
        )
        if (!dialog.showAndGet()) {
            return
        }
        val request = dialog.getPublishRequest() ?: return

        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveAllDocuments()
        }
        startBatchPublish(project, request.packages, request.includePublishDate)
    }

    override fun update(e: AnActionEvent) {
        val rootDirectory = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = e.project != null &&
            rootDirectory != null &&
            rootDirectory.isDirectory &&
            PubPackagePublishUtil.hasMultipleChildPackages(rootDirectory)
        e.presentation.text = PluginBundle.get("batch_publish_child_packages_action")
        e.presentation.icon = AllIcons.Actions.Upload
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun startBatchPublish(
        project: com.intellij.openapi.project.Project,
        packages: List<BatchPublishPackageRequest>,
        includePublishDate: Boolean
    ) {
        val output = StringBuilder()
        val failures = mutableListOf<String>()
        var successCount = 0
        var currentProcessHandler: OSProcessHandler? = null

        fun showResultNotification() {
            val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("dio_socket_notify")
                .createNotification(
                    if (failures.isEmpty()) {
                        PluginBundle.get("batch_publish_child_packages_success", successCount.toString())
                    } else {
                        PluginBundle.get("batch_publish_child_packages_failed", failures.size.toString())
                    },
                    if (failures.isEmpty()) NotificationType.INFORMATION else NotificationType.ERROR
                )
            notification.icon = MyIcons.flutter
            notification.addAction(object : com.intellij.openapi.project.DumbAwareAction(
                PluginBundle.get("pubspec_notification_view_output")
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    val outputText = synchronized(output) {
                        output.toString().ifBlank { PluginBundle.get("pubspec_notification_no_output") }
                    }
                    CommandOutputDialog(
                        project,
                        PluginBundle.get("batch_publish_child_packages_output_title"),
                        outputText
                    ).show()
                    notification.hideBalloon()
                    notification.expire()
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }
            })
            notification.notify(project)
        }

        fun appendPackageOutputHeader(request: BatchPublishPackageRequest) {
            synchronized(output) {
                output.appendLine("===== ${request.packageInfo.name} =====")
                output.appendLine("version: ${request.version}")
                output.appendLine("directory: ${request.packageInfo.workDirectory.absolutePath}")
                output.appendLine("command: dart pub publish --force")
                output.appendLine()
            }
        }

        project.launchBackgroundProgress(PluginBundle.get("batch_publish_child_packages_task_title")) {
            try {
                val progressContext = coroutineContext
                reportRawProgress { reporter ->
                    packages.forEachIndexed { index, request ->
                        progressContext.ensureActive()
                        reporter.fraction(index.toDouble() / packages.size.coerceAtLeast(1))
                        reporter.text(PluginBundle.get("batch_publish_child_packages_task_running"))
                        reporter.details("${index + 1}/${packages.size} · ${request.packageInfo.name}")
                        appendPackageOutputHeader(request)

                        try {
                            PubPackagePublishUtil.updatePubspecVersion(
                                request.packageInfo.workDirectory,
                                request.packageInfo.name,
                                request.version
                            )
                            PubPackagePublishUtil.updateChangelogForPublish(
                                request.packageInfo.workDirectory,
                                request.version,
                                request.changelog,
                                includePublishDate
                            )
                            val commandLine = GeneralCommandLine("dart", "pub", "publish", "--force")
                                .withWorkDirectory(request.packageInfo.workDirectory)
                            val handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                            currentProcessHandler = handler
                            ProcessTerminatedListener.attach(handler)
                            handler.addProcessListener(object : ProcessListener {
                                override fun startNotified(event: com.intellij.execution.process.ProcessEvent) = Unit

                                override fun processTerminated(event: com.intellij.execution.process.ProcessEvent) = Unit

                                override fun processWillTerminate(
                                    event: com.intellij.execution.process.ProcessEvent,
                                    willBeDestroyed: Boolean
                                ) = Unit

                                override fun onTextAvailable(
                                    event: com.intellij.execution.process.ProcessEvent,
                                    outputType: com.intellij.openapi.util.Key<*>
                                ) {
                                    synchronized(output) {
                                        output.append(event.text)
                                    }
                                }
                            })
                            handler.startNotify()
                            while (!handler.waitFor(500)) {
                                progressContext.ensureActive()
                            }
                            val exitCode = handler.exitCode ?: -1
                            if (exitCode == 0) {
                                successCount += 1
                            } else {
                                failures += "${request.packageInfo.name} (exit code: $exitCode)"
                            }
                        } catch (error: CancellationException) {
                            currentProcessHandler?.destroyProcess()
                            throw error
                        } catch (error: Exception) {
                            failures += "${request.packageInfo.name}: ${error.message ?: "unknown error"}"
                            synchronized(output) {
                                output.append(error.stackTraceToString()).appendLine()
                            }
                        }
                    }
                    reporter.fraction(1.0)
                }
                showResultNotification()
            } catch (error: CancellationException) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("dio_socket_notify")
                    .createNotification(
                        PluginBundle.get("batch_publish_child_packages_cancelled"),
                        NotificationType.WARNING
                    )
                    .notify(project)
                throw error
            }
        }
    }
}
