package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.source.tree.PsiCommentImpl
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.i18n.PluginBundle

/**
 * 给方法快速生成文档
 */
class GenerateFunctionDocument : MyAction() {


    override fun update(e: AnActionEvent) {
        e.presentation.text = PluginBundle.get("generate.fun.comment")
        val psi = e.getData(CommonDataKeys.PSI_ELEMENT)
        e.presentation.isEnabled = psi != null && psi.parent is DartMethodDeclarationImpl
        super.update(e)
    }

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
                    sb.append("/// [$name] - ${if (psis.last() == it) "" else "\n"}")
                }
                e.project?.let {
                    createDartDocPsiElement(it, data, sb.toString())
                }
            }
        }
    }

    private fun createDartDocPsiElement(
        project: Project,
        element: PsiElement,
        text: String
    ) {
        val funDefine = element.parent as? DartMethodDeclarationImpl ?: return
        val classMe = funDefine.parent as? DartClassMembersImpl ?: return

        val docList = createDoc(text, project)
        WriteCommandAction.runWriteCommandAction(project) {
            for (doc in docList) {
                classMe.addBefore(doc, funDefine)
            }
        }


    }

    fun createDoc(text: String, project: Project): List<PsiCommentImpl> {
        val psiFile = PsiFileFactory.getInstance(project).createFileFromText(DartLanguage.INSTANCE, text)
        return PsiTreeUtil.findChildrenOfAnyType(psiFile, PsiCommentImpl::class.java).toList()
    }
}

