package shop.itbug.fluttercheckversionx.widget

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.constance.discordUrl
import shop.itbug.fluttercheckversionx.dialog.MyTextDialog
import shop.itbug.fluttercheckversionx.dialog.RewardDialog
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.toast
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
     * 帮助组件
     */
    fun getHelpActionToolbar(name: String, action: (e: AnActionEvent) -> Unit): ActionToolbar {
        fun createRightActions(): Array<AnAction> = arrayOf(
            getHelpAnAction(action)
        )
        return ActionManager.getInstance()
            .createActionToolbar(name, DefaultActionGroup(*createRightActions()), true)
    }


    /**
     * 输入弹窗
     */
    fun configTextFieldModal(
        project: Project,
        labelText: String,
        comment: String? = null,
        handle: (text: String) -> Unit
    ) {
        val dialog = MyTextDialog(
            project = project, label = labelText,
            comment = comment,
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
     * 文档操作
     */
    fun getDocAnAction(link: String): AnAction {
        return object : MyDumbAwareAction(AllIcons.Actions.Help) {
            override fun actionPerformed(e: AnActionEvent) {
                BrowserUtil.browse(link)
            }
        }
    }

    /**
     * 获取复制文本组件
     * @param copyText 要复制的文本
     */
    fun getCopyAnAction(copyText: String): AnAction {
        return object : MyDumbAwareAction(AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) {
                copyText.copyTextToClipboard()
                e.project?.apply {
                    toast("Copy succeeded!")
                }.takeIf { copyText.trim().isNotEmpty() }
            }
        }
    }


    /**
     * 反馈论坛
     */
    fun getDiscordAction(): AnAction {
        return object : MyDumbAwareAction(MyIcons.discord) {
            override fun actionPerformed(p0: AnActionEvent) {
                BrowserUtil.browse(discordUrl)
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

    /**
     * 打赏组件
     */
    fun getMoneyAnAction(): AnAction {
        return object : DumbAwareAction(MyIcons.money) {
            override fun actionPerformed(e: AnActionEvent) {
                e.project?.let {
                    RewardDialog(it).show()
                }
            }
        }
    }
}