package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.PlatformCodeVisionIds
import com.intellij.codeInsight.hints.codeVision.InheritorsCodeVisionProvider
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.constance.MyKeys
import shop.itbug.fluttercheckversionx.services.PubspecService
import java.awt.event.MouseEvent


/**
 * 是否需要对psi 处理?
 */
private fun PsiElement.needHandler(): Boolean {
    val setting = PluginConfig.getState(project)
    val hasRiverpod = PubspecService.getInstance(project).hasRiverpod()
    val element = this
    return setting.showRiverpodInlay && element is DartClassDefinitionImpl && hasRiverpod && (element.superclass?.type?.text == "StatelessWidget" || element.superclass?.type?.text == "StatefulWidget")
}


class DartWidgetToRiverpodWidgetCodeVisit : InheritorsCodeVisionProvider() {
    override fun acceptsFile(file: PsiFile): Boolean {
        return file is DartFile
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        return element.needHandler()
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        return "Riverpod Tool"
    }

    private val group = ActionManager.getInstance().getAction("WidgetToRiverpod") as DefaultActionGroup
    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        event ?: return
        editor.putUserData(MyKeys.DartClassKey, element as DartClassDefinitionImpl)
        val context = DataManager.getInstance().getDataContext(editor.component)
        val popupMenu = ActionManager.getInstance().createActionPopupMenu("Riverpod Tool Menu", group)
        popupMenu.setDataContext { context }
        JBPopupMenu.showByEvent(event, popupMenu.component)
    }

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = emptyList()
    override val id: String
        get() = "Simple Widget to Riverpod Widget"

    override val name: String
        get() = "Simple Widget to Riverpod Widget"

    override val groupId: String
        get() = PlatformCodeVisionIds.INHERITORS.key
}