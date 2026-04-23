package shop.itbug.flutterx.notif

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.flutterx.common.yaml.PubspecYamlFileTools
import shop.itbug.flutterx.constance.DartPubMirrorImage
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.dialog.CommandOutputDialog
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.services.PubCacheSizeCalcService
import shop.itbug.flutterx.services.PubCacheSizeCalcService.Companion.TOPIC
import shop.itbug.flutterx.setting.IgPluginPubspecConfigList
import shop.itbug.flutterx.tools.MyToolWindowTools
import shop.itbug.flutterx.util.MyActionUtil
import shop.itbug.flutterx.util.MyFileUtil
import shop.itbug.flutterx.util.toast
import shop.itbug.flutterx.util.toastWithError
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.function.Function
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

class PubPluginVersionCheckNotification : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        return Function<FileEditor, JComponent?> {
            if (project.isDisposed) return@Function null
            if (!PluginConfig.getState(project).showPubspecYamlNotificationBar) return@Function null
            if (it.component.parent == null) return@Function null
            if (file.name != "pubspec.yaml") return@Function null
            val psiFile = PsiManager.getInstance(project).findFile(file) as? YAMLFile ?: return@Function null

            val isFlutterProject =
                runBlocking(Dispatchers.IO) { PubspecYamlFileTools.create(psiFile).isFlutterProject() }

            if (!isFlutterProject) return@Function null
            return@Function YamlFileNotificationPanel(it, psiFile, project)
        }
    }

}


private class YamlFileNotificationPanel(fileEditor: FileEditor, val file: YAMLFile, val project: Project) :
    EditorNotificationPanel(fileEditor, UIUtil.getEditorPaneBackground()) {

    private val pubCacheSizeComponent = MyCheckPubCacheSizeComponent(project)
    private val moreActionGroup = DefaultActionGroup().apply {
        add(object : DumbAwareAction(PluginBundle.get("pubspec_notification_hide_toolbar")) {
            override fun actionPerformed(e: AnActionEvent) {
                PluginConfig.changeState(project) {
                    it.showPubspecYamlNotificationBar = false
                }
                EditorNotifications.getInstance(project).updateNotifications(file.virtualFile)
            }

            override fun getActionUpdateThread(): ActionUpdateThread {
                return ActionUpdateThread.BGT
            }
        })
    }

    init {
        Disposer.register(PubCacheSizeCalcService.getInstance(project), pubCacheSizeComponent)

        myLinksPanel.add(pubCacheSizeComponent)

        icon(MyIcons.dartPluginIcon)
        text(PluginBundle.get("w.t"))

        val searchPluginLabel = createActionLabel(PluginBundle.get("search.pub.plugin")) {
            search()
        }
        myLinksPanel.add(searchPluginLabel)

        lateinit var chinaMirrorPubGetLabel: HyperlinkLabel
        chinaMirrorPubGetLabel = createActionLabel(PluginBundle.get("pubspec_notification_run_pub_get_with_mirror")) {
            showChinaMirrorPopup(chinaMirrorPubGetLabel)
        }
        myLinksPanel.add(chinaMirrorPubGetLabel)

        if (shouldShowPublishAction()) {
            lateinit var publishToPubDevLabel: HyperlinkLabel
            publishToPubDevLabel = createActionLabel(PluginBundle.get("pubspec_notification_publish_to_pub_dev")) {
                showPublishPopup(publishToPubDevLabel)
            }
            myLinksPanel.add(publishToPubDevLabel)
        }

        ///重新索引
        val reIndexLabel = createActionLabel(PluginBundle.get("pubspec_yaml_file_re_index")) {
            MyFileUtil.reIndexWithVirtualFile(file.virtualFile)
            DaemonCodeAnalyzer.getInstance(project).restart(file)
        }

        myLinksPanel.add(reIndexLabel)


        ///打开隐私扫描窗口
        val openPrivacyWindowLabel =
            createActionLabel(PluginBundle.get("are_you_ok_betch_insert_privacy_file_window_title")) {
                doOpenPrivacyWindow()
            }

        myLinksPanel.add(openPrivacyWindowLabel)


        ///管理忽略的包
        val igPackageLabel = createActionLabel(PluginBundle.get("pub.dev.search.ignore.packages")) {
            IgPluginPubspecConfigList.showInPopup(project, file)
        }
        myLinksPanel.add(igPackageLabel)

        initMoreMenu()
    }

    ///打开隐私扫描工具窗口
    private fun doOpenPrivacyWindow() {
        val myToolWindow = MyToolWindowTools.getMyToolWindow(project)
        myToolWindow?.let {
            it.activate {
                val content = it.contentManager.getContent(4)
                if (content != null) {
                    it.contentManager.setSelectedContent(content)
                }
            }
        }
    }

    private fun search() {
        MyActionUtil.showPubSearchDialog(project,file)
    }

    private fun showChinaMirrorPopup(anchor: JComponent) {
        val actionGroup = DefaultActionGroup().apply {
            DartPubMirrorImage.chinaMirrors.forEach { mirror ->
                add(object : DumbAwareAction(mirror.title) {
                    override fun actionPerformed(e: AnActionEvent) {
                        runFlutterPubGetWithMirror(mirror)
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread {
                        return ActionUpdateThread.BGT
                    }
                })
            }
        }
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                PluginBundle.get("pubspec_notification_select_china_mirror"),
                actionGroup,
                DataManager.getInstance().getDataContext(anchor),
                JBPopupFactory.ActionSelectionAid.MNEMONICS,
                true
            )
            .showUnderneathOf(anchor)
    }

    private fun showPublishPopup(anchor: JComponent) {
        val currentVersion =
            currentPubspecVersion() ?: PluginBundle.get("pubspec_notification_publish_version_unknown")
        val textArea = JBTextArea().apply {
            rows = 10
            lineWrap = true
            wrapStyleWord = true
        }
        val headerPanel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(
                JBLabel(PluginBundle.get("pubspec_notification_publish_popup_version", currentVersion)),
                BorderLayout.NORTH
            )
            add(JBLabel(PluginBundle.get("pubspec_notification_publish_popup_hint")), BorderLayout.SOUTH)
        }
        val includePublishDateCheckbox =
            JBCheckBox(PluginBundle.get("pubspec_notification_publish_popup_include_date"), false)
        val publishButton = JButton(PluginBundle.get("pubspec_notification_publish_popup_button"))
        val contentPanel = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(12)
            add(headerPanel, BorderLayout.NORTH)
            add(
                JBScrollPane(textArea).apply {
                    preferredSize = JBUI.size(460, 260)
                },
                BorderLayout.CENTER
            )
            add(
                JPanel(BorderLayout()).apply {
                    add(includePublishDateCheckbox, BorderLayout.WEST)
                    add(publishButton, BorderLayout.EAST)
                },
                BorderLayout.SOUTH
            )
        }

        lateinit var popup: JBPopup
        publishButton.addActionListener {
            val releaseNotes = textArea.text.trim()
            if (releaseNotes.isBlank()) {
                project.toastWithError(PluginBundle.get("pubspec_notification_publish_changelog_empty"))
                return@addActionListener
            }
            popup.cancel()
            runDartPubPublish(releaseNotes, includePublishDateCheckbox.isSelected)
        }

        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(contentPanel, textArea)
            .setTitle(PluginBundle.get("pubspec_notification_publish_popup_title"))
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelKeyEnabled(true)
            .createPopup()
        popup.showUnderneathOf(anchor)
    }

    private fun currentPubspecVersion(): String? {
        return currentPubspecValue("version")
    }

    private fun shouldShowPublishAction(): Boolean {
        val publishTo = currentPubspecValue("publish_to") ?: return true
        return !publishTo.equals("none", ignoreCase = true)
    }

    private fun currentPubspecValue(key: String): String? {
        return runBlocking(Dispatchers.IO) {
            PubspecYamlFileTools.create(file)
                .getRootKeyValueList()
                ?.firstOrNull { it.keyText.trim() == key }
                ?.valueText
                ?.trim()
                ?.removeSurrounding("\"")
                ?.removeSurrounding("'")
                ?.takeIf { it.isNotBlank() }
        }
    }

    private fun runDartPubPublish(releaseNotes: String, includePublishDate: Boolean) {
        val workDirectory = file.virtualFile.parent?.toNioPath()?.toFile()
        if (workDirectory == null) {
            project.toastWithError(PluginBundle.get("pubspec_notification_pub_get_directory_not_found"))
            return
        }

        ApplicationManager.getApplication().runWriteAction {
            FileDocumentManager.getInstance().saveAllDocuments()
        }
        val version = currentPubspecVersion()
        if (version.isNullOrBlank()) {
            project.toastWithError(PluginBundle.get("pubspec_notification_publish_version_not_found"))
            return
        }

        object : Task.Backgroundable(
            project,
            PluginBundle.get("pubspec_notification_publish_task_title", version),
            true
        ) {
            private var exitCode: Int = -1
            private var startError: String? = null
            private var isCancelled = false
            private var processHandler: OSProcessHandler? = null
            private val output = StringBuilder()

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = PluginBundle.get("pubspec_notification_publish_task_running", version)
                indicator.text2 = "CHANGELOG.md"

                try {
                    updateChangelogForPublish(workDirectory, version, releaseNotes, includePublishDate)
                    indicator.text2 = "dart pub publish --force"

                    val commandLine =
                        GeneralCommandLine("dart", "pub", "publish", "--force").withWorkDirectory(workDirectory)
                    val handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                    processHandler = handler
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
                        indicator.checkCanceled()
                    }
                    exitCode = handler.exitCode ?: -1
                } catch (_: ProcessCanceledException) {
                    isCancelled = true
                    processHandler?.destroyProcess()
                    throw ProcessCanceledException()
                } catch (e: Exception) {
                    startError = e.message ?: version
                }
            }

            override fun onSuccess() {
                if (startError != null) {
                    showPublishResultNotification(
                        NotificationType.ERROR,
                        PluginBundle.get("pubspec_notification_publish_start_failed", startError ?: version)
                    )
                    return
                }
                if (exitCode == 0) {
                    showPublishResultNotification(
                        NotificationType.INFORMATION,
                        PluginBundle.get("pubspec_notification_publish_success", version)
                    )
                } else {
                    showPublishResultNotification(
                        NotificationType.ERROR,
                        PluginBundle.get("pubspec_notification_publish_failed", version, exitCode.toString())
                    )
                }
            }

            override fun onCancel() {
                if (isCancelled) {
                    showPublishResultNotification(
                        NotificationType.WARNING,
                        PluginBundle.get("pubspec_notification_publish_cancelled", version)
                    )
                }
            }

            private fun showPublishResultNotification(
                type: NotificationType,
                content: String
            ) {
                val outputText = synchronized(output) {
                    output.toString().ifBlank { PluginBundle.get("pubspec_notification_no_output") }
                }
                val notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("dio_socket_notify")
                    .createNotification(content, type)
                notification.icon = MyIcons.flutter
                notification.addAction(object : DumbAwareAction(PluginBundle.get("pubspec_notification_view_output")) {
                    override fun actionPerformed(e: AnActionEvent) {
                        CommandOutputDialog(
                            project,
                            PluginBundle.get("pubspec_notification_output_dialog_title", "dart pub publish"),
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
        }.queue()
    }

    private fun updateChangelogForPublish(
        workDirectory: File,
        version: String,
        releaseNotes: String,
        includePublishDate: Boolean
    ) {
        val changelogFile = File(workDirectory, "CHANGELOG.md")
        if (!changelogFile.exists()) {
            throw IllegalStateException(PluginBundle.get("pubspec_notification_publish_changelog_not_found"))
        }

        val originalContent = changelogFile.readText(Charsets.UTF_8)
        val lineSeparator = detectLineSeparator(originalContent)
        val normalizedNotes = normalizeReleaseNotes(releaseNotes, lineSeparator)
        val updatedContent = insertOrUpdateChangelogSection(
            originalContent,
            version,
            normalizedNotes,
            lineSeparator,
            includePublishDate
        )

        changelogFile.writeText(updatedContent, Charsets.UTF_8)
        VirtualFileManager.getInstance().asyncRefresh(null)
    }

    private fun insertOrUpdateChangelogSection(
        content: String,
        version: String,
        releaseNotes: String,
        lineSeparator: String,
        includePublishDate: Boolean
    ): String {
        val heading = buildChangelogHeading(content, version, includePublishDate)
        val versionHeaderRegex = Regex(
            "(?m)^## (\\[${Regex.escape(version)}\\]|${Regex.escape(version)})(?:\\s+-.*)?$"
        )
        val existingVersionMatch = versionHeaderRegex.find(content)
        if (existingVersionMatch != null) {
            val nextHeaderRegex = Regex("(?m)^## ")
            val nextHeaderMatch = nextHeaderRegex.find(content, existingVersionMatch.range.last + 1)
            val sectionEnd = nextHeaderMatch?.range?.first ?: content.length
            val existingSection = content.substring(existingVersionMatch.range.first, sectionEnd)
            val existingBody = existingSection.substringAfter(existingVersionMatch.value).trim()
            val mergedSection = buildString {
                append(heading)
                append(lineSeparator)
                append(lineSeparator)
                append(releaseNotes)
                if (existingBody.isNotBlank()) {
                    append(lineSeparator)
                    append(lineSeparator)
                    append(existingBody)
                }
                if (nextHeaderMatch != null) {
                    append(lineSeparator)
                    append(lineSeparator)
                }
            }
            return ensureTrailingLineBreak(
                content.replaceRange(existingVersionMatch.range.first, sectionEnd, mergedSection),
                lineSeparator
            )
        }

        val unreleasedRegex = Regex("(?m)^## Unreleased(?:\\r?\\n)*")
        val unreleasedMatch = unreleasedRegex.find(content)
        if (unreleasedMatch == null) {
            val prependedContent = buildString {
                append(heading)
                append(lineSeparator)
                append(lineSeparator)
                append(releaseNotes)
                val remainingContent = content.trimStart('\r', '\n')
                if (remainingContent.isNotBlank()) {
                    append(lineSeparator)
                    append(lineSeparator)
                    append(remainingContent)
                }
            }
            return ensureTrailingLineBreak(prependedContent, lineSeparator)
        }
        val insertedContent = buildString {
            append("## Unreleased")
            append(lineSeparator)
            append(lineSeparator)
            append(heading)
            append(lineSeparator)
            append(lineSeparator)
            append(releaseNotes)
            append(lineSeparator)
            append(lineSeparator)
        }
        return ensureTrailingLineBreak(
            content.replaceRange(unreleasedMatch.range.first, unreleasedMatch.range.last + 1, insertedContent),
            lineSeparator
        )
    }

    private fun buildChangelogHeading(content: String, version: String, includePublishDate: Boolean): String {
        val usesBracketVersion = Regex("(?m)^## \\[").containsMatchIn(content)
        val publishDateSuffix = if (includePublishDate) {
            " - ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        } else {
            ""
        }
        return if (usesBracketVersion) {
            "## [$version]$publishDateSuffix"
        } else {
            "## $version$publishDateSuffix"
        }
    }

    private fun normalizeReleaseNotes(releaseNotes: String, lineSeparator: String): String {
        return releaseNotes
            .trim()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .joinToString(lineSeparator)
    }

    private fun detectLineSeparator(text: String): String = if (text.contains("\r\n")) "\r\n" else "\n"

    private fun ensureTrailingLineBreak(text: String, lineSeparator: String): String {
        return if (text.endsWith(lineSeparator)) text else text + lineSeparator
    }

    private fun runFlutterPubGetWithMirror(mirror: DartPubMirrorImage) {
        val workDirectory = file.virtualFile.parent?.toNioPath()?.toFile()
        if (workDirectory == null) {
            project.toastWithError(PluginBundle.get("pubspec_notification_pub_get_directory_not_found"))
            return
        }
        val flutterStorageBaseUrl = mirror.flutterStorageBaseUrl
        if (flutterStorageBaseUrl.isNullOrBlank()) {
            project.toastWithError(PluginBundle.get("pubspec_notification_pub_get_start_failed", mirror.title))
            return
        }

        object : Task.Backgroundable(
            project,
            PluginBundle.get("pubspec_notification_pub_get_task_title", mirror.title),
            true
        ) {
            private var exitCode: Int = -1
            private var startError: String? = null
            private var isCancelled = false
            private var processHandler: OSProcessHandler? = null
            private val output = StringBuilder()

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = PluginBundle.get("pubspec_notification_pub_get_task_running", mirror.title)
                indicator.text2 = "${mirror.title} · flutter pub get"

                try {
                    val commandLine = GeneralCommandLine("flutter", "pub", "get").withWorkDirectory(workDirectory)
                    commandLine.withEnvironment("PUB_HOSTED_URL", mirror.url)
                    commandLine.withEnvironment("FLUTTER_STORAGE_BASE_URL", flutterStorageBaseUrl)

                    val handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                    processHandler = handler
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
                        indicator.checkCanceled()
                    }
                    exitCode = handler.exitCode ?: -1
                } catch (_: ProcessCanceledException) {
                    isCancelled = true
                    processHandler?.destroyProcess()
                    throw ProcessCanceledException()
                } catch (e: Exception) {
                    startError = e.message ?: mirror.title
                }
            }

            override fun onSuccess() {
                if (startError != null) {
                    showPubGetResultNotification(
                        NotificationType.ERROR,
                        PluginBundle.get("pubspec_notification_pub_get_start_failed", startError ?: mirror.title),
                        mirror
                    )
                    return
                }
                if (exitCode == 0) {
                    showPubGetResultNotification(
                        NotificationType.INFORMATION,
                        PluginBundle.get("pubspec_notification_pub_get_success", mirror.title),
                        mirror
                    )
                } else {
                    showPubGetResultNotification(
                        NotificationType.ERROR,
                        PluginBundle.get(
                            "pubspec_notification_pub_get_failed",
                            mirror.title,
                            exitCode.toString()
                        ),
                        mirror
                    )
                }
            }

            override fun onCancel() {
                if (isCancelled) {
                    showPubGetResultNotification(
                        NotificationType.WARNING,
                        PluginBundle.get("pubspec_notification_pub_get_cancelled", mirror.title),
                        mirror
                    )
                }
            }
            private fun showPubGetResultNotification(
                type: NotificationType,
                content: String,
                mirror: DartPubMirrorImage
            ) {
                val outputText = synchronized(output) {
                    output.toString().ifBlank { PluginBundle.get("pubspec_notification_no_output") }
                }
                val notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("dio_socket_notify")
                    .createNotification(content, type)
                notification.icon = MyIcons.flutter
                notification.addAction(object : DumbAwareAction(PluginBundle.get("pubspec_notification_view_output")) {
                    override fun actionPerformed(e: AnActionEvent) {
                        CommandOutputDialog(
                            project,
                            PluginBundle.get("pubspec_notification_output_dialog_title", mirror.title),
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
        }.queue()
    }

    private fun initMoreMenu() {
        myGearLabel.isVisible = true
        myGearLabel.icon = AllIcons.General.Settings
        myGearLabel.border = JBUI.Borders.emptyLeft(12)
        myGearLabel.toolTipText = PluginBundle.get("menu")
        myGearLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        myGearLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                showMoreMenu()
            }
        })
    }

    private fun showMoreMenu() {
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                PluginBundle.get("menu"),
                moreActionGroup,
                DataManager.getInstance().getDataContext(myGearLabel),
                JBPopupFactory.ActionSelectionAid.MNEMONICS,
                true
            )
            .showUnderneathOf(myGearLabel)
    }

}


///计算pub cache 占用大小
private class MyCheckPubCacheSizeComponent(project: Project) : HyperlinkLabel(), PubCacheSizeCalcService.Listener,
    Disposable, BulkFileListener {
    val cacheService = PubCacheSizeCalcService.getInstance(project)
    private var isDisposed = false

    init {
        project.messageBus.connect(cacheService).subscribe(TOPIC, this)
        project.messageBus.connect(parentDisposable = this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            this
        )
        Disposer.register(cacheService, this)
        SwingUtilities.invokeLater {
            setDefaultText()
        }
        ToolTipManager.sharedInstance().registerComponent(this)
        toolTipText = cacheService.getPubCacheDirPathString()
        ApplicationManager.getApplication().invokeLater {
            cacheService.refreshCheck()
        }
    }

    override fun fireHyperlinkEvent(inputEvent: InputEvent?) {
        cacheService.openDir()
        super.fireHyperlinkEvent(inputEvent)
    }


    private fun setDefaultText() {
        setHyperlinkText(PluginBundle.get("pub.cache.size", cacheService.getCurrentSizeFormatString()))
    }

    override fun calcComplete(len: Long, formatString: String) {
        SwingUtilities.invokeLater {
            if (!isDisposed) {
                setHyperlinkText(PluginBundle.get("pub.cache.size", formatString))
            }

        }

    }

    override fun dispose() {
        println("dispose pub cache size widget")
        isDisposed = true
        ToolTipManager.sharedInstance().unregisterComponent(this)
    }

    override fun after(events: List<VFileEvent>) {
        val cachePath = cacheService.getCachePath()
        if (events.isNotEmpty() && cachePath.isNullOrBlank()
                .not() && events.any { it.file?.path?.startsWith(cachePath) == true }
        ) {
            cacheService.refreshCheck()
        }
        super.after(events)
    }
}
