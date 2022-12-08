package shop.itbug.fluttercheckversionx.render

import shop.itbug.fluttercheckversionx.model.chat.IdeaMessage
import javax.swing.DefaultListModel

class ChatHistoryListModel(val list: List<IdeaMessage>) : DefaultListModel<IdeaMessage>() {

    override fun getSize(): Int {
       return list.size
    }

    override fun getElementAt(index: Int): IdeaMessage {
        return list[index]
    }


}