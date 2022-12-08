package shop.itbug.fluttercheckversionx.render

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.model.chat.IdeaMessage
import shop.itbug.fluttercheckversionx.widget.AvatarIcon
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Box
import javax.swing.JList
import javax.swing.JPanel
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
        val box1 = Box.createHorizontalBox()
        box1.add(JBLabel(AvatarIcon(20,20,model.user.picture)))
        box1.add(JBLabel(model.user.nickName))
        add(box1,BorderLayout.NORTH)
        add(JBLabel(model.content),BorderLayout.CENTER)
    }
}