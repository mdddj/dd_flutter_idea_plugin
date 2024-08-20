package shop.itbug.fluttercheckversionx.actions.internal

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.impl.DartCallExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartReturnStatementImpl
import com.jetbrains.lang.dart.util.DartElementGenerator
import shop.itbug.fluttercheckversionx.icons.MyIcons
import javax.swing.Icon

abstract class WrapWithInternalBase : PsiElementBaseIntentionAction(), Iconable {

    abstract fun title(): String

    abstract fun getReplaceText(element: PsiElement): String


    override fun getIcon(flags: Int): Icon {
        return MyIcons.flutter
    }

    override fun getFamilyName(): String {
        return title()
    }

    override fun getText(): String {
        return familyName
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val returnEle = DartPsiTreeUtil.findReturn(element)
        return returnEle != null && returnEle.expression != null && createElement(returnEle.expression!!) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val returnEle: DartReturnStatementImpl = DartPsiTreeUtil.findReturn(element) ?: return
        val create = createElement(returnEle.expression!!)
        create?.let { returnEle.expression?.replace(it) }
    }


    private fun createElement(element: PsiElement): DartCallExpressionImpl? {
        val text = """
final widget = ${getReplaceText(element)}
        """.trimIndent()
        val file = DartElementGenerator.createDummyFile(element.project, text)
        return PsiTreeUtil.findChildOfType(file, DartCallExpressionImpl::class.java)
    }
}