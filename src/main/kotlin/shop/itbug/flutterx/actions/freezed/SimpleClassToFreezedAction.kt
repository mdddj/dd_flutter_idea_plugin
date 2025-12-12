package shop.itbug.flutterx.actions.freezed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.jetbrains.lang.dart.psi.impl.DartMethodDeclarationImpl
import shop.itbug.flutterx.dialog.SimpleClassToFreezedActionDialog
import shop.itbug.flutterx.i18n.PluginBundle

//简单构造函数转成freezed对象
class SimpleClassToFreezedAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)?.parent as DartMethodDeclarationImpl
        SimpleClassToFreezedActionDialog(project, psiElement).show()
    }


    override fun update(e: AnActionEvent) {
        e.presentation.text = PluginBundle.get("simple_class_to_freezed_object_f1")
        val project = e.project
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        println(psiElement)
        e.presentation.isEnabled =
            psiElement != null && project != null && psiElement.parent is DartMethodDeclarationImpl
        super.update(e)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
