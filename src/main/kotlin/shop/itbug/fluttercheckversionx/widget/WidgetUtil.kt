package shop.itbug.fluttercheckversionx.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.dialog.MyTextDialog
import java.awt.Component
import java.awt.Point
import javax.swing.JComponent


object WidgetUtil {

    /**
     * 弹窗一个窗口,让用户输入值,然后在[onSubmit] 获取到这个值
     */
    fun getTextEditorPopup(
        title: String,
        placeholder: String,
        initValue: String? = null,
        preferableFocusComponent: JComponent? = null,
        show: (popup: JBPopup) -> Unit,
        onSubmit: MySimpleTextFieldSubmit,
    ) {
        lateinit var popup: JBPopup
        popup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(MySimpleTextField(placeholder = placeholder, initValue) {
                    onSubmit.invoke(it)
                    popup.cancel()
                }, preferableFocusComponent)
                .setTitle(title)
                .setCancelKeyEnabled(true)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setFocusable(true)
                .createPopup()
        show.invoke(popup)
    }


    /**
     * 输入弹窗
     */
    fun configTextFieldModal(
        project: Project,
        labelText: String,
        comment: String? = null,
        initValue: String? = null,
        handle: (text: String) -> Unit
    ) {
        val dialog = MyTextDialog(
            project = project, label = labelText,
            comment = comment,
            initValue = initValue,
            handle
        )
        dialog.show()
    }

    /**
     * 帮助服务操作组
     * @param action 点击帮助图标执行事件
     */
    fun getHelpAnAction(action: (e: AnActionEvent) -> Unit): AnAction {
        return object : MyDumbAwareAction(AllIcons.Actions.Help) {
            override fun actionPerformed(e: AnActionEvent) {
                action.invoke(e)
            }
        }
    }


    /**
     * 显示一个气球提示文本
     */
    fun showTopBalloon(component: Component, text: String) {
        JBPopupFactory.getInstance().createBalloonBuilder(JBLabel(text))
            .setContentInsets(JBUI.insets(10)).createBalloon().show(
                RelativePoint(component, Point(component.width / 2, 0)), Balloon.Position.above
            )
    }

}