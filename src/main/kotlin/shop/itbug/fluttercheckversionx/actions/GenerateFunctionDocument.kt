package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartFormalParameterListImpl
import com.jetbrains.lang.dart.psi.impl.DartNormalFormalParameterImpl

///生成方法的函数文档操作
class GenerateFunctionDocument : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val data = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (data is DartComponentNameImpl) {
            val nextSibling = data.nextSibling
            if (nextSibling is DartFormalParameterListImpl) {
                val psis = PsiTreeUtil.collectElements(
                    nextSibling
                ) { element -> element is DartNormalFormalParameterImpl }
                val sb = StringBuilder()
                sb.append("///\n")
                psis.forEach {
                    val name = it.firstChild.lastChild.text
                    sb.append("/// [$name] - \n")
                }.takeIf { psis.isNotEmpty() }
                e.project?.let {
                    createDartDocPsiElement(it, data.parent, sb.toString())
                }
            }
        }
    }

    private fun createDartDocPsiElement(project: Project, element: PsiElement, text: String) {
        println("进来了.")
        val psifile = PsiFileFactory.getInstance(project).createFileFromText(DartLanguage.INSTANCE, text)
        WriteCommandAction.runWriteCommandAction(project) {
            element.addBefore(psifile.navigationElement,element)
        }

    }
}
