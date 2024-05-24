package shop.itbug.fluttercheckversionx.actions.components

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.ex.CustomComponentAction

abstract class MyButtonAnAction(val text: String) : AnAction(text), CustomComponentAction {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}