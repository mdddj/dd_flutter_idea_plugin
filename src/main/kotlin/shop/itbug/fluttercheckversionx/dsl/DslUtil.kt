package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.BorderFactory

/**
 * 弹窗
 */
fun DialogPanel.showWithPoint(point: Point) {
    JBPopupFactory.getInstance().createComponentPopupBuilder(this,null)
        .createPopup().show(RelativePoint(point))
}

fun DialogPanel.showCenter(project: Project) {
    JBPopupFactory.getInstance().createComponentPopupBuilder(this,null)
        .createPopup().showCenteredInCurrentWindow(project)
}

fun DialogPanel.show(anActionEvent: AnActionEvent) {
    JBPopupFactory.getInstance().createComponentPopupBuilder(this,null)
        .createPopup().show(RelativePoint(anActionEvent.inputEvent.component.locationOnScreen))
}

/**
 * 给组件添加边框
 */
fun DialogPanel.addBorder() : DialogPanel {
    return this.withBorder(BorderFactory.createEmptyBorder(DslConfig.padding, DslConfig.padding, DslConfig.padding, DslConfig.padding))
}