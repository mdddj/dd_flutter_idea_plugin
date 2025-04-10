package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint

fun DialogPanel.show(anActionEvent: AnActionEvent) {
    anActionEvent.inputEvent?.let {
        JBPopupFactory.getInstance().createComponentPopupBuilder(this, null)
            .createPopup().show(RelativePoint(it.component.locationOnScreen))
    }

}

