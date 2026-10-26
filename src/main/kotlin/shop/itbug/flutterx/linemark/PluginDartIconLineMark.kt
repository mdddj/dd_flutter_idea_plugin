package shop.itbug.flutterx.linemark

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.ui.awt.RelativePoint
import icons.MyImages
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.flutterx.actions.PUB_URL
import shop.itbug.flutterx.cache.YamlFileIgDartPackageCache
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.services.PubService
import shop.itbug.flutterx.util.DartPackageDirectoryUtil
import shop.itbug.flutterx.tools.YAML_DART_PACKAGE_INFO_KEY
import shop.itbug.flutterx.tools.YAML_FILE_IS_FLUTTER_PROJECT
import shop.itbug.flutterx.util.MyFileUtil
import shop.itbug.flutterx.util.getPluginName
import shop.itbug.flutterx.util.isDartPluginElement
import java.awt.event.MouseEvent
import javax.swing.Icon


class PluginDartIconLineMark : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>,
    ) {
        if (elements.isEmpty()) return
        val psiFile = elements.first().containingFile as? YAMLFile ?: return
        if (psiFile.name != PUBSPEC_FILE_NAME) return
        if (psiFile.getUserData(YAML_FILE_IS_FLUTTER_PROJECT) != true) return
        val virtualFile = psiFile.virtualFile ?: return
        val project = psiFile.project

        // 启动恢复编辑器时，高亮线程持有读锁，EDT 还拿着文档锁。
        // 这里创建 LineMarkerInfo 或首次读取项目服务会让两边互相等待，界面无法关闭。
        if (!StartupManager.getInstance(project).postStartupActivityPassed()) {
            return
        }
        val document = FileDocumentManager.getInstance().getCachedDocument(virtualFile)
        if (document != null && !PsiDocumentManager.getInstance(project).isCommitted(document)) {
            return
        }

        ProgressManager.checkCanceled()
        val ignoreCache = YamlFileIgDartPackageCache.getInstance(project)
        for (element in elements) {
            if (!element.isDartPluginElement()) continue
            val anchor = element.leafAnchor() ?: continue
            val packageName = element.getPluginName()
            val isIgnored = ignoreCache.state.hasItem(psiFile, packageName)
            result.add(
                LineMarkerInfo(
                    anchor,
                    anchor.textRange,
                    if (isIgnored) MyImages.ignore else MyIcons.dartPackageIcon,
                    { packageName },
                    PluginDartIconLineMarkNavHandler(element, psiFile),
                    GutterIconRenderer.Alignment.LEFT,
                ) { packageName }
            )
        }
    }
}

private const val PUBSPEC_FILE_NAME = "pubspec.yaml"
private const val PUBSPEC_GUTTER_RESTART_REASON = "FlutterX pubspec gutter icons"

/**
 * 启动阶段的高亮会跳过排水沟图标。项目打开后再刷新一次已打开的 pubspec.yaml。
 */
class PubspecGutterIconStartupActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        if (project.isDisposed) return
        readAction {
            if (project.isDisposed) return@readAction
            val analyzer = DaemonCodeAnalyzer.getInstance(project)
            for (virtualFile in FileEditorManager.getInstance(project).openFiles) {
                if (virtualFile.name != PUBSPEC_FILE_NAME) continue
                val file = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
                if (file.isValid) {
                    analyzer.restart(file, PUBSPEC_GUTTER_RESTART_REASON)
                }
            }
        }
    }
}

private fun PsiElement.leafAnchor(): PsiElement? {
    var current = firstChild ?: return null
    var depth = 0
    while (depth < 32) {
        val child = current.firstChild ?: return current
        current = child
        depth++
    }
    return current
}

class PluginDartIconLineMarkNavHandler(val element: PsiElement, val file: YAMLFile) :
    GutterIconNavigationHandler<PsiElement> {
    override fun navigate(e: MouseEvent?, elt: PsiElement?) {
        if ((e != null) && (e.clickCount == 1)) {
            JBPopupFactory.getInstance().createListPopup(PluginDartIconActionMenuList(element = element, file))
                .show(RelativePoint(e.locationOnScreen))
        }
    }
}

data class PluginDartIconActionMenuItem(val title: String, val type: String, val icon: Icon)

class PluginDartIconActionMenuList(val element: PsiElement, val file: YAMLFile) :
    BaseListPopupStep<PluginDartIconActionMenuItem>() {

    private val project = element.project
    private val ignoreServices = YamlFileIgDartPackageCache.getInstance(project)
    private val removeIgnoreText = PluginBundle.get("ignore_remove")
    private val addIgnoreText = PluginBundle.get("ig.version.check")
    private val pluginName = element.getPluginName()
    private val isIgnored = ignoreServices.state.hasItem(file, pluginName)


    private val menus: MutableList<PluginDartIconActionMenuItem>
        get() {
            val arr = mutableListOf(
                PluginDartIconActionMenuItem(
                    title = "${PluginBundle.get("nav.to")} pub.dev",
                    type = "navToPub",
                    icon = AllIcons.Toolwindows.WebToolWindow
                ),
                PluginDartIconActionMenuItem(
                    if (isIgnored) removeIgnoreText else addIgnoreText,
                    "ig-check",
                    icon = MyImages.ignore
                ),
                PluginDartIconActionMenuItem(
                    PluginBundle.get("pub.dev.menu.open.directory"),
                    "open-directory",
                    icon = AllIcons.General.OpenDisk
                ),
                PluginDartIconActionMenuItem(
                    PluginBundle.get("pub.dev.menu.show.json"),
                    "open-json-text",
                    icon = AllIcons.FileTypes.Json
                ),
                PluginDartIconActionMenuItem(
                    PluginBundle.get("pub.dev.menu.open.api"),
                    "open-api-in-browser",
                    icon = AllIcons.Toolwindows.WebToolWindow
                )
            )
            return arr
        }

    init {
        super.init(pluginName, menus, menus.map { it.icon })
    }


    override fun getTextFor(value: PluginDartIconActionMenuItem?): String {
        return value?.title ?: PluginBundle.get("unknown.option")
    }

    override fun onChosen(selectedValue: PluginDartIconActionMenuItem?, finalChoice: Boolean): PopupStep<*>? {
        when (selectedValue?.type) {
            menus[0].type -> {
                BrowserUtil.browse("$PUB_URL${pluginName}")
            }

            menus[1].type -> {
                if (isIgnored) {
                    ignoreServices.state.remove(file, pluginName)
                } else {
                    ignoreServices.state.addNew(file, pluginName)
                }
                DaemonCodeAnalyzer.getInstance(project).restart(file, "FlutterX pubspec gutter ignore changed")
                MyFileUtil.reIndexFile(project, file.virtualFile)
            }

            menus[2].type -> {
                DartPackageDirectoryUtil.openInstalledPackageDirectory(project, pluginName)
            }

            menus[3].type -> {
                //打开json文件
                ApplicationManager.getApplication().invokeLater {
                    val infos = file.getUserData(YAML_DART_PACKAGE_INFO_KEY) ?: return@invokeLater
                    val detail = infos.find { it.name == pluginName } ?: return@invokeLater
                    val jsonText = detail.pubData?.jsonText ?: return@invokeLater
                    if (jsonText.isNotEmpty()) {
                        MyFileUtil.createVirtualFileByJsonText(jsonText, "${pluginName}.json") { file, tool ->
                            tool.openInEditor(file, project)
                            tool.reformatVirtualFile(file, project)
                        }
                    }
                }
            }

            menus[4].type -> {
                BrowserUtil.browse(PubService.getApiUrl(pluginName))
            }
        }
        return super.onChosen(selectedValue, finalChoice)
    }


}
