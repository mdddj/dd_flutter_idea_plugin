package shop.itbug.fluttercheckversionx.actions.internal

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import shop.itbug.fluttercheckversionx.services.PubspecService

class ProviderWrapInternaleAction : WrapWithInternalBase() {
    override fun title(): String {
        return "Wrap with Consumer (provider)"
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val v = super.isAvailable(project, editor, element)
        return v && PubspecService.getInstance(project).hasProvider()
    }

    override fun getReplaceText(element: PsiElement): String {
        return """
            Consumer(builder: (context, value, child) {
      return ${element.text};
    },)
        """.trimIndent()
    }
}