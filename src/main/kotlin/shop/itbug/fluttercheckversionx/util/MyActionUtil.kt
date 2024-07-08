package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar

fun ActionGroup.toolbar(place: String): ActionToolbar {
    return ActionManager.getInstance().createActionToolbar(place, this, true)
}
