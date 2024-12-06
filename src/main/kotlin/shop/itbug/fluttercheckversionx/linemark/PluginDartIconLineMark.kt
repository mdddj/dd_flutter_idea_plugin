package shop.itbug.fluttercheckversionx.linemark

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import icons.MyImages
import shop.itbug.fluttercheckversionx.actions.PUB_URL
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.DartPackageTaskParam
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.noused.DartNoUsedCheckService
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.getPluginName
import shop.itbug.fluttercheckversionx.util.isDartPluginElement
import shop.itbug.fluttercheckversionx.util.restartAnalyzer
import java.awt.event.MouseEvent
import javax.swing.Icon


class PluginDartIconLineMark : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if (element.isDartPluginElement()) {
            val packageName = element.getPluginName()
            val igManager = DartPluginIgnoreConfig.getInstance(element.project)
            val isIgnored = igManager.isIg(packageName)
            return LineMarkerInfo(
                element.firstChild,
                element.firstChild.textRange,
                if (isIgnored) MyImages.ignore else MyIcons.dartPackageIcon,
                { element.text },
                PluginDartIconLineMarkNavHandler(element),
                GutterIconRenderer.Alignment.LEFT
            ) { "" }
        }
        return null
    }
}

class PluginDartIconLineMarkNavHandler(val element: PsiElement) : GutterIconNavigationHandler<PsiElement> {
    override fun navigate(e: MouseEvent?, elt: PsiElement?) {
        if ((e != null) && (e.clickCount == 1)) {
            JBPopupFactory.getInstance().createListPopup(PluginDartIconActionMenuList(element = element))
                .show(RelativePoint(e.locationOnScreen))
        }
    }
}

data class PluginDartIconActionMenuItem(val title: String, val type: String, val icon: Icon)

class PluginDartIconActionMenuList(val element: PsiElement) : BaseListPopupStep<PluginDartIconActionMenuItem>() {

    private val project = element.project
    private val virtualFile = element.containingFile.virtualFile
    private val removeIgnoreText = PluginBundle.get("ignore_remove")
    private val addIgnoreText = PluginBundle.get("ig.version.check")
    private val igManager = DartPluginIgnoreConfig.getInstance(project)
    private val pluginName = element.getPluginName()
    private val isIgnored = igManager.isIg(pluginName)
    private val detail = DartPackageCheckService.getInstance(project).findPackageInfoByName(pluginName)

    private val menus: MutableList<PluginDartIconActionMenuItem>
        get() {
            val arr = mutableListOf<PluginDartIconActionMenuItem>(
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
                    "Open Directory",
                    "open-directory",
                    icon = AllIcons.General.OpenDisk
                ),
                PluginDartIconActionMenuItem(
                    "Show Json Data",
                    "open-json-text",
                    icon = AllIcons.FileTypes.Json
                ),
                PluginDartIconActionMenuItem(
                    "Open package api in browser",
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
        return value?.title ?: "未知选项"
    }

    fun reIndexFile() {
        project.restartAnalyzer()
        MyFileUtil.reIndexFile(project, virtualFile)
    }

    override fun onChosen(selectedValue: PluginDartIconActionMenuItem?, finalChoice: Boolean): PopupStep<*>? {
        when (selectedValue?.type) {
            menus[0].type -> {
                BrowserUtil.browse("$PUB_URL${pluginName}")
            }

            menus[1].type -> {
                if (isIgnored) {
                    DartPluginIgnoreConfig.getInstance(project).remove(pluginName)
                    val params = DartPackageTaskParam(false) {
                        reIndexFile()
                    }
                    DartPackageCheckService.getInstance(project).startResetIndex(params)
                } else {
                    DartPluginIgnoreConfig.getInstance(project).add(pluginName)
                    DartPackageCheckService.getInstance(project).removeItemByPluginName(pluginName)
                    reIndexFile()
                }


            }

            menus[2].type -> {
                DartNoUsedCheckService.getInstance(project).openInBrowser(pluginName)
            }

            menus[3].type -> {
                //打开json文件
                ApplicationManager.getApplication().invokeLater {
                    val jsonText = detail?.second?.jsonText
                    if (jsonText != null) {
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