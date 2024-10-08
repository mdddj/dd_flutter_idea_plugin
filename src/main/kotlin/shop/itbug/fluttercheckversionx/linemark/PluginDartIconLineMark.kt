package shop.itbug.fluttercheckversionx.linemark

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
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

///处理点击
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

    private val removeIgnoreText = PluginBundle.get("ignore_remove")
    private val addIgnoreText = PluginBundle.get("ig.version.check")
    private val igManager = DartPluginIgnoreConfig.getInstance(element.project)
    private val pluginName = element.getPluginName()
    private val isIgnored = igManager.isIg(pluginName)

    private val menus
        get() = listOf(
            PluginDartIconActionMenuItem(
                title = "${PluginBundle.get("nav.to")} pub.dev",
                type = "navToPub",
                icon = AllIcons.Toolwindows.WebToolWindow
            ),
            PluginDartIconActionMenuItem(
                if (isIgnored) removeIgnoreText else addIgnoreText,
                "ig-check",
                icon = MyImages.ignore
            )
        )

    init {
        super.init(element.text, menus, menus.map { it.icon })
    }


    override fun getTextFor(value: PluginDartIconActionMenuItem?): String {
        return value?.title ?: "未知选项"
    }

    override fun onChosen(selectedValue: PluginDartIconActionMenuItem?, finalChoice: Boolean): PopupStep<*>? {
        when (selectedValue?.type) {
            menus[0].type -> {
                BrowserUtil.browse("$PUB_URL${pluginName}")
            }

            menus[1].type -> {
                if (isIgnored) {
                    DartPluginIgnoreConfig.getInstance(element.project).remove(pluginName)
                } else {
                    DartPluginIgnoreConfig.getInstance(element.project).add(pluginName)
                }

                element.project.restartAnalyzer()
            }
        }
        return super.onChosen(selectedValue, finalChoice)
    }


}