package shop.itbug.fluttercheckversionx.actions.freezed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.manager.DartFactoryConstructorDeclarationImplManager


fun AnActionEvent.getFactoryPsiElement(): DartFactoryConstructorDeclarationImpl? {
    val psiElement = getData(CommonDataKeys.PSI_ELEMENT)
    val isFactoryName = psiElement?.parent is DartFactoryConstructorDeclarationImpl
    return if (isFactoryName) psiElement.parent as DartFactoryConstructorDeclarationImpl else null
}


///给属性设置默认值
class FreezedFactoryAddDefaultValue : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val manager = DartFactoryConstructorDeclarationImplManager(e.getFactoryPsiElement()!!)
        manager.setAllPropertiesToDefaultValue()
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getFactoryPsiElement() != null
        e.presentation.text = PluginBundle.get("set.as.freezed.default.value")
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
