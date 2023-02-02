package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.dialog.FreezedClassesGenerateDialog
import shop.itbug.fluttercheckversionx.services.impl.ModelToFreezedModelServiceImpl
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.getDartClassDefinition

/**
 * 模型转freezed
 */
class FxModelToFreezedModel : AnAction() {

    private val toFreezedService = ModelToFreezedModelServiceImpl()
    override fun actionPerformed(e: AnActionEvent) {
        val model = toFreezedService.anActionEventToFreezedCovertModel(e)
//        FreezedCovertDialog(e.project!!, model).show()
        FreezedClassesGenerateDialog(e.project!!, mutableListOf(model))
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getDartClassDefinition() != null && DartPsiElementUtil.getClassProperties(e.getDartClassDefinition()!!).isNotEmpty()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
