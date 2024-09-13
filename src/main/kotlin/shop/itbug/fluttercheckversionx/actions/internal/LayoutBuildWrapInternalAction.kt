package shop.itbug.fluttercheckversionx.actions.internal

import com.intellij.psi.PsiElement

class LayoutBuildWrapInternalAction : WrapWithInternalBase() {
    override fun title(): String {
        return "Wrap with LayoutBuild"
    }

    override fun getReplaceText(element: PsiElement): String {
        return """
            LayoutBuilder(builder: (context, constraints) {
      return ${element.text};
    })
        """.trimIndent()
    }
}