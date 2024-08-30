package shop.itbug.fluttercheckversionx.inlay.base

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

data class MyBaseInlayModel(
    val psiFile: PsiFile,
    val editor: Editor,
    val project: Project,
    val element: PsiElement
)

fun MyBaseInlayModel.getLineNumber(): Int {
    return editor.document.getLineNumber(element.textRange.startOffset)
}


abstract class MyBaseInlay : InlayHintsProvider {


    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector? {
        return object : SharedBypassCollector {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
                val context = MyBaseInlayModel(file, editor, element.project, element)
                if (needCollect(context)) {
                    handle(context, sink)
                }
            }
        }
    }

    /**
     * 检测是否处理 [PsiElement],如果返回 true,则执行 [handle]
     * @param context 上下文模型
     */
    abstract fun needCollect(context: MyBaseInlayModel): Boolean


    abstract fun handle(context: MyBaseInlayModel, sink: InlayTreeSink)
}