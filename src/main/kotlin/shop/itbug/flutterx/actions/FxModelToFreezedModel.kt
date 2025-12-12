package shop.itbug.flutterx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.flutterx.common.MyAction
import shop.itbug.flutterx.dialog.FreezedCovertDialog
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.services.impl.ModelToFreezedModelServiceImpl
import shop.itbug.flutterx.util.DartPsiElementUtil
import shop.itbug.flutterx.util.getDartClassDefinition

/**
 * 模型转freezed
 */
class FxModelToFreezedModel : MyAction() {

    private val toFreezedService = ModelToFreezedModelServiceImpl()
    override fun actionPerformed(e: AnActionEvent) {
        val model = toFreezedService.anActionEventToFreezedCovertModel(e)
        FreezedCovertDialog(e.project!!, model).show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled =
            e.getDartClassDefinition() != null && DartPsiElementUtil.getClassProperties(e.getDartClassDefinition()!!)
                .isNotEmpty()

        e.presentation.text = PluginBundle.get("class.to.object")
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}
