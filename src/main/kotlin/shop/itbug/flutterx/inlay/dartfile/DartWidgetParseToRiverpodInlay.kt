package shop.itbug.flutterx.inlay.dartfile

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.PlatformCodeVisionIds
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.codeInsight.hints.settings.language.isInlaySettingsEditor
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SyntaxTraverser
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.constance.MyKeys
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.services.PubspecService
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


class DartWidgetToRiverpodWidgetCodeVisit : CodeVisionProviderBase() {
    override fun acceptsFile(file: PsiFile): Boolean {
        val project = file.project
        val config = PluginConfig.getState(project)
        val enable = config.showRiverpodInlay
        if (!enable) return false
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

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        if (file.project.isDefault) return emptyList()
        if (!acceptsFile(file)) return emptyList()

        // we want to let this provider work only in tests dedicated for code vision, otherwise they harm performance
        if (ApplicationManager.getApplication().isUnitTestMode && !CodeVisionHost.isCodeLensTest()) return emptyList()

        val virtualFile = file.viewProvider.virtualFile
        if (ProjectFileIndex.getInstance(file.project).isInLibrarySource(virtualFile)) return emptyList()

        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()
        val traverser = SyntaxTraverser.psiTraverser(file)
        for (element in traverser) {
            if (!acceptsElement(element)) continue
            val hint = getHint(element, file)
            if (hint == null) continue
            val range = MyHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
            val handler = ClickHandler(element, hint)
            lenses.add(
                range to ClickableTextCodeVisionEntry(
                    hint,
                    id,
                    handler,
                    icon = MyIcons.flutter,
                    tooltip = "Riverpod tool"
                )
            )
        }
        return lenses
    }

    private inner class ClickHandler(
        element: PsiElement,
        private val hint: String,
    ) : (MouseEvent?, Editor) -> Unit {
        private val elementPointer = SmartPointerManager.createPointer(element)

        override fun invoke(event: MouseEvent?, editor: Editor) {
            if (isInlaySettingsEditor(editor)) return
            val element = elementPointer.element ?: return
            logClickToFUS(element, hint)
            handleClick(editor, element, event)
        }
    }

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = emptyList()
    override val id: String
        get() = "Simple Widget to Riverpod Widget"

    override val name: String
        get() = "Simple Widget to Riverpod Widget"

    override val groupId: String
        get() = PlatformCodeVisionIds.INHERITORS.key

    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Top
}