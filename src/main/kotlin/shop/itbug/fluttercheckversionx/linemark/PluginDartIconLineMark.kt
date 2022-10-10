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
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.actions.PUB_URL
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.getPluginName
import shop.itbug.fluttercheckversionx.util.isDartPluginElement
import java.awt.event.MouseEvent
import javax.swing.Icon

class PluginDartIconLineMark : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? {
        if ( element.isDartPluginElement() && element is YAMLKeyValueImpl) {
            return LineMarkerInfo(
                element.firstChild, element.firstChild.textRange,
                MyIcons.dartPackageIcon, { element.text }, PluginDartIconLineMarkNavHandler(element),
                GutterIconRenderer.Alignment.LEFT
            ) { "111" }
        }
        return null
    }
}

///处理点击
class PluginDartIconLineMarkNavHandler(val element: PsiElement) : GutterIconNavigationHandler<PsiElement> {
    override fun navigate(e: MouseEvent?, elt: PsiElement?) {
        if ((e != null) && (e.clickCount == 1)) {
            JBPopupFactory.getInstance().createListPopup(PluginDartIconActioinMenuList(element = element))
                .show(RelativePoint(e.locationOnScreen))
        }
    }
}

data class PluginDartIconActionMenuItem(val title: String, val type: String, val icon: Icon)
class PluginDartIconActioinMenuList(val element: PsiElement) : BaseListPopupStep<PluginDartIconActionMenuItem>() {
    private val menus
        get() = listOf(
            PluginDartIconActionMenuItem(
                title = "跳转到pub.dev", type = "navToPub", icon = AllIcons.Toolwindows.WebToolWindow
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
                BrowserUtil.browse("$PUB_URL${element.getPluginName()}")
            }
        }
        return super.onChosen(selectedValue, finalChoice)
    }
}