package shop.itbug.fluttercheckversionx.widget

import com.aallam.openai.api.BetaOpenAI
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import shop.itbug.fluttercheckversionx.common.toJsonFormart
import shop.itbug.fluttercheckversionx.dsl.docPanel
import shop.itbug.fluttercheckversionx.inlay.MyAIChatModel
import shop.itbug.fluttercheckversionx.util.OpenAiUtil
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.ListCellRenderer

class AiChatListWidget(val project: Project) : JBList<MyAIChatModel>() {

    val mutex = Mutex()

    init {
        model = DefaultListModel()
        cellRenderer =
            ListCellRenderer { _, p1, _, _, _ ->
                panel {
                    row(if (p1.isMe) "Q:" else "AI:") {
                        if(p1.isMe){
                            label(p1.content.toString())
                        }else{
                            scrollCell(docPanel(p1.content.toString(),project))
                        }

                    }
                }
            }

        border = BorderFactory.createEmptyBorder()
    }


    fun addQ(content: String) {
        val q = MyAIChatModel(content = java.lang.StringBuilder(content), isMe = true)
        getListModel().addElement(q)
        startChat(content)
    }

    /**
     * 开始一个会话
     */
    @OptIn(BetaOpenAI::class, DelicateCoroutinesApi::class)
    fun startChat(content: String) {
        GlobalScope.launch(Dispatchers.IO) {
            OpenAiUtil.askSimple(content).collect { chunk ->
                val r = chunk.choices.first().delta?.content ?: ""
                if(r.isNotEmpty()){
                    val index = getListModel().toArray().indexOfLast { obj ->
                        val it = obj as MyAIChatModel
                        it.id == chunk.id && it.id.isNotEmpty() && it.isMe.not()
                    }
                    mutex.withLock {
                        if (index != -1) {
                            val ele = getListModel()[index]
                            ele.content.append(r)
                            getListModel()[index] = ele
                            println(getListModel().toArray().toJsonFormart())
                        } else {
                            val newChat = MyAIChatModel(
                                content = StringBuilder(r),
                                id = chunk.id
                            )
                            getListModel().addElement(newChat)

                        }
                    }
                }
            }
        }
    }


    private fun getListModel(): DefaultListModel<MyAIChatModel> {
        return model as DefaultListModel<MyAIChatModel>
    }


}