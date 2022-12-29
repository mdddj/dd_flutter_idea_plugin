package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartPsiCompositeElement
import com.jetbrains.lang.dart.psi.impl.DartBlockImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartFormalParameterListImpl
import com.jetbrains.lang.dart.psi.impl.DartNormalFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartPsiCompositeElementImpl
import com.jetbrains.lang.dart.util.DartElementGenerator
import com.jetbrains.lang.dart.util.DartPsiImplUtil
import io.flutter.dart.DartPsiUtil
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElementFactory

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
                }
            }
        }
    }
}