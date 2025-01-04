package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.PlatformCodeVisionIds
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.InlayHintsUtils
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.codeInsight.hints.settings.language.isInlaySettingsEditor
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SyntaxTraverser
import com.jetbrains.lang.dart.psi.DartFile
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper
import shop.itbug.fluttercheckversionx.util.SwingUtil
import java.awt.event.MouseEvent

/**
 * dart assets icon render
 */
class DartAssetsIconInlineShow : CodeVisionProviderBase() {
    override fun acceptsFile(file: PsiFile): Boolean {
        return file is DartFile && PluginConfig.getInstance(file.project).state.showAssetsIconInEditor
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        val file = DartPsiElementHelper.checkHasFile(element)
        return file != null
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        val file = DartPsiElementHelper.checkHasFile(element) ?: return null
        return " ${file.file.name}"
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

//            if (!InlayHintsUtils.isFirstInLine(element)) continue
            val hint = getHint(element, file)
            if (hint == null) continue
            val range = InlayHintsUtils.getTextRangeWithoutLeadingCommentsAndWhitespaces(element)
            val handler = ClickHandler(element, hint)
            val file = DartPsiElementHelper.checkHasFile(element)?.file ?: continue
            val icon = SwingUtil.fileToIcon(file, PluginConfig.getInstance(element.project).state.assetsIconSize)
            lenses.add(
                range to ClickableTextCodeVisionEntry(
                    hint,
                    id,
                    handler,
                    icon = icon,
                )
            )
        }
        return lenses
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        val findFile = DartPsiElementHelper.checkHasFile(element) ?: return
        val file = runReadAction { LocalFileSystem.getInstance().findFileByIoFile(findFile.file) }
        if (file != null) {
            ProjectView.getInstance(element.project).select(null, file, true) //文件浏览器中打开
        }
    }

    override val name: String
        get() = "Dart Assets Icon Inline"
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = emptyList()
    override val id: String
        get() = "DartAssetsIconInlineShow"

    override val groupId: String
        get() = PlatformCodeVisionIds.INHERITORS.key


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
}