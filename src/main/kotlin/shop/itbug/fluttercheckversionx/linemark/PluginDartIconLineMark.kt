package shop.itbug.fluttercheckversionx.linemark

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import java.awt.event.MouseEvent
import javax.swing.Icon

class PluginDartIconLineMark : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*> {
        if (element is YAMLKeyValueImpl) {
            return LineMarkerInfo(
                element, element.textRange,
                MyIcons.dartPluginIcon, { element.text }, PluginDartIconLineMarkNavHandler(element),
                GutterIconRenderer.Alignment.LEFT
            ) { "111" }
        }
        return LineMarkerInfo(element, element.textRange)
    }
}

///处理点击
class PluginDartIconLineMarkNavHandler(val element: PsiElement) : GutterIconNavigationHandler<PsiElement> {

    override fun navigate(e: MouseEvent?, elt: PsiElement?) {
        if (e != null && e.clickCount == 1) {
            JBPopupFactory.getInstance().createListPopup(PluginDartIconActioinMenuList(element = element))
                .show(RelativePoint(e.locationOnScreen))
        }
    }

}

data class PluginDartIconActionMenuItem(val title: String, val type: String, val icon: Icon)
class PluginDartIconActioinMenuList(element: PsiElement) : BaseListPopupStep<String>() {
    private val menus
        get() = listOf(
            PluginDartIconActionMenuItem(
                title = "跳转到pub.dev", type = "navToPub", icon = AllIcons.Toolwindows.WebToolWindow
            )
        )

    init {
        super.init(element.text, menus.map { it.title }, menus.map { it.icon })
    }
}