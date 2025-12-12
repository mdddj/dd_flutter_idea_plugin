package shop.itbug.flutterx.inlay.dartfile

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.PlatformCodeVisionIds
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.find.actions.ShowUsagesAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import java.awt.event.MouseEvent


/**
 * 统计dart element 使用次数
 */
class DartUseageCountInlay : CodeVisionProviderBase() {
    override fun acceptsFile(file: PsiFile): Boolean {
        return file is DartFile
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        return (element is DartClassDefinitionImpl)
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        if (element is DartClassDefinitionImpl) {
            return "${element.componentName.getUsagesCount()} usages"
        }
        return null
    }

    override fun handleClick(
        editor: Editor, element: PsiElement, event: MouseEvent?
    ) {
        event?.let {
            ShowUsagesAction.startFindUsages(element, RelativePoint(it), editor)
        }

    }

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter("Simple Widget to Riverpod Widget"))
    override val id: String
        get() = "Dart Usages Count"

    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Default


    override val name: String
        get() = "Dart usage Count"

    override val groupId: String
        get() = PlatformCodeVisionIds.USAGES.key

}

// 获取psi被使用次数
private fun PsiElement.getUsagesCount(): Int {
    return ReferencesSearch.search(this, GlobalSearchScope.allScope(project)).findAll().size
}


