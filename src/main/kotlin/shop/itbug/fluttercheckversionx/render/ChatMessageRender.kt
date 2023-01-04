package shop.itbug.fluttercheckversionx.render

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.model.chat.IdeaMessage
import shop.itbug.fluttercheckversionx.widget.AvatarIcon
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * 聊天消息的渲染样式
 */
class ChatMessageRender : ListCellRenderer<IdeaMessage> {

    override fun getListCellRendererComponent(
        list: JList<out IdeaMessage>?,
        value: IdeaMessage?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if(value == null) return JBLabel()
        val panel = ChatModelRender(value)
        return panel
    }

}

class ChatModelRender(val model: IdeaMessage): JBPanel<ChatModelRender>(BorderLayout()) {
    init {
//        val box1 = Box.createHorizontalBox()
//        box1.add(JBLabel(AvatarIcon(20,20,model.user.picture)))
//        box1.add(JBLabel(model.user.nickName))
//        add(box1,BorderLayout.NORTH)
//        add(JBLabel(model.content),BorderLayout.CENTER)
        add(chatLayoutPanel(model))
    }
}

fun chatLayoutPanel(model: IdeaMessage) : DialogPanel {
    val avatar = JBLabel(AvatarIcon(22,22,model.user.picture))
    return panel {
        row {
            cell(avatar).gap(RightGap.SMALL)
            label(model.user.nickName).gap(RightGap.SMALL).apply {
                component.font = JBFont.h4()
            }
            label(model.createTime).apply {
                component.font = JBFont.small()
                component.foreground = UIUtil.getLabelForeground()
            }
        }
        row {
            label(model.content).apply {
                component.font = JBFont.medium()
            }
        }
        row {
            button("分享了一段Dart代码"){

            }
        }.visible(model.code.isEmpty())
    }
}