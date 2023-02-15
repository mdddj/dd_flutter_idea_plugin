package shop.itbug.fluttercheckversionx.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import javax.swing.JComponent


object WidgetUtil {


    /**
     * 弹出一个输入框,并获取内容
     */
    fun getTextEditorPopup(
        title: String,
        placeholder: String,
        show: (popup: JBPopup) -> Unit,
        onSubmit: MySimpleTextFieldSubmit,
    ) {
        lateinit var popup: JBPopup
        popup =
            JBPopupFactory.getInstance().createComponentPopupBuilder(MySimpleTextField(placeholder = placeholder) {
                onSubmit.invoke(it)
                popup.cancel()
            }, null)
                .setRequestFocus(true)
                .setTitle(title)
                .setCancelKeyEnabled(true)
                .setResizable(false)
                .setMovable(true)
                .createPopup()
        show.invoke(popup)
    }

    /**
     * 帮助组件
     */
    fun getHelpIconComponent(name: String, action: (e: AnActionEvent) -> Unit): JComponent {
        fun createRightActions(): Array<AnAction> = arrayOf(
            object : DumbAwareAction(AllIcons.Actions.Help) {
                override fun actionPerformed(e: AnActionEvent) {
                    action.invoke(e)
                }
            }
        )
        val toolbar = ActionManager.getInstance()
            .createActionToolbar(name, DefaultActionGroup(*createRightActions()), true)
        return toolbar.component
    }
}