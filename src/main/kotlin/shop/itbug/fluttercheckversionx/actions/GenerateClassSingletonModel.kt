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
