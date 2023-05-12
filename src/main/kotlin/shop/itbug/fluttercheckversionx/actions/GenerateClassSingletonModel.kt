package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.createDialogAndShow
import shop.itbug.fluttercheckversionx.dialog.showCodePreviewDialog
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.reformat


///在编辑器当前光标下插入一个新的psi element
fun AnActionEvent.insetNewPsiElementWithCurrentOffset(newPsiElement: PsiElement) {
    val editor = this.getData(CommonDataKeys.EDITOR)!!
    val document: Document = editor.document
    val offset = editor.caretModel.offset
    val element = PsiDocumentManager.getInstance(project!!).getPsiFile(document)!!
        .findElementAt(offset)?.prevSibling
    element?.apply {
        WriteCommandAction.runWriteCommandAction(project) {
            this.addAfter(newPsiElement, this.nextSibling)
            project.reformat(getData(CommonDataKeys.PSI_ELEMENT)!!)
        }
    }
}


///创建单例类
class GenerateClassSingletonModel : MyAction() {

    override fun actionPerformed(e: AnActionEvent) {
        var className = ""
        val result = e.project?.createDialogAndShow("Create a singleton object") {
            return@createDialogAndShow panel {
                row("Class name") {
                    textField().bindText({ className }, { className = it })
                }
            }
        }
        if (result == true) {
            val classObject = MyDartPsiElementUtil.genClassConstructor(e.project!!, className)!!
            e.project?.showCodePreviewDialog(classObject.firstChild.text)
        }

    }

}
