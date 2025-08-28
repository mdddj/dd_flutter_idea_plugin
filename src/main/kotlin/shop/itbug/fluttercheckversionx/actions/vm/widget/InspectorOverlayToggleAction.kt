package shop.itbug.fluttercheckversionx.actions.vm.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import kotlinx.coroutines.launch
import vm.setInspectorOverlay


/**
 * Inspector Overlay 开关Action
 * 用于控制Flutter应用中的调试图层显示/隐藏
 * 支持实时监听状态变化
 */
class InspectorOverlayToggleAction() : ToggleAction(
    "Toggle Inspector Overlay",
    "Show/Hide Flutter Inspector Overlay on device",
    AllIcons.General.InspectionsEye
) {


    override fun isSelected(e: AnActionEvent): Boolean {
        return e.flutterTree.getInspectorStateManager()?.getCurrentState() ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val tree = e.flutterTree
        tree.scope.launch {
            try {
                val isolateId = tree.isolateId
                if (isolateId != null) {
                    val isSuccess = tree.vmService.setInspectorOverlay(isolateId, state)
                    if (isSuccess) {
                        tree.getInspectorStateManager()?.updateState(state)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = e.flutterTree.isolateId != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

}