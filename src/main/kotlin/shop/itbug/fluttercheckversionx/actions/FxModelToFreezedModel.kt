package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import shop.itbug.fluttercheckversionx.dialog.FreezedCovertDialog
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.getDartClassDefinition

/**
 * 模型转freezed
 */
class FxModelToFreezedModel : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val dartClassDefinition = e.getDartClassDefinition()!!
        val project = e.getData(CommonDataKeys.PROJECT)!!
        val classProperties = DartPsiElementUtil.getClassProperties(dartClassDefinition)
        val models = DartPsiElementUtil.getModels(classProperties)
        FreezedCovertDialog(project, FreezedCovertModel(properties = models, className = dartClassDefinition.componentName.text)).show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getDartClassDefinition() != null && DartPsiElementUtil.getClassProperties(e.getDartClassDefinition()!!).isNotEmpty()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
