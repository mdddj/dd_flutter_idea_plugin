package shop.itbug.fluttercheckversionx.actions.components

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.util.ui.JButtonAction

abstract class MyButtonAnAction(val text: String) : JButtonAction(text), CustomComponentAction {


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}