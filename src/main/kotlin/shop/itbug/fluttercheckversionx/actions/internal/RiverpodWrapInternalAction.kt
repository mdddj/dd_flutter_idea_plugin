package shop.itbug.fluttercheckversionx.actions.internal

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import shop.itbug.fluttercheckversionx.services.PubspecService

/// provider的 consumer


class RiverpodWrapInternalAction : WrapWithInternalBase() {

    override fun title(): String {
        return "Wrap with Consumer (riverpod)"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val v = super.isAvailable(project, editor, element)
        return v && PubspecService.getInstance(project).hasRiverpod()
    }

    override fun getReplaceText(element: PsiElement): String {
        return """
            Consumer(builder: (context, ref, child) {
              return ${element.text};
            })
        """.trimIndent()
    }
}