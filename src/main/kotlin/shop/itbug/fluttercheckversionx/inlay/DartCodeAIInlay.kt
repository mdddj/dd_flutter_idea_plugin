package shop.itbug.fluttercheckversionx.inlay

import com.aallam.openai.api.BetaOpenAI
import com.intellij.codeInsight.hints.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import com.intellij.util.lateinitVal
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.psi.impl.DartMethodDeclarationImpl
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.actions.DartAiSwitchAction
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.dialog.AiApiKeyConfigDialog
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.OpenAiUtil
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities


data class DartAIConfig(var showInEditor: Boolean = true)

@State(name = "DartAIConfig", storages = [Storage("DartAIConfig.xml")])
class DartAISetting private constructor() : PersistentStateComponent<DartAIConfig> {
    private var st = DartAIConfig()
    override fun getState(): DartAIConfig {
        return st
    }

    override fun loadState(state: DartAIConfig) {
        st = state
    }

    companion object {
        fun getInstance() = service<DartAISetting>()
    }
}


class DartCodeAIInlay : InlayHintsProvider<DartAISetting>, Disposable {

    override val key: SettingsKey<DartAISetting>
        get() = SettingsKey(name)

    override val name: String
        get() = "Open-AI-Setting"
    override val previewText: String
        get() {
            return """
                class TestClass {
                
                    void test() {
                    
                    }
                }
            """.trimIndent()
        }

    override fun createSettings(): DartAISetting {
        return DartAISetting.getInstance()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: DartAISetting,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {


                val isShow = settings.state.showInEditor

                val offset = element.textRange.startOffset
                val line = editor.document.getLineNumber(offset)
                val lineStart = editor.document.getLineStartOffset(line)
                val indent = offset - lineStart //缩进
                val indentText = StringUtil.repeat(" ", indent)
                if (element is DartMethodDeclarationImpl && isShow) {
                    val text = factory.text("$indentText  ")
                    val icon = factory.smallScaledIcon(MyIcons.openai)

                    val ai = factory.smallText(" 求助AI")
                    val newF = factory.seq(text, factory.roundWithBackgroundAndSmallInset(factory.seq(icon, ai)))
                    val click = factory.mouseHandling(
                        newF,
                        { event, _ ->
                            showDialogPanel(event)
                        }, null
                    )
                    sink.addBlockElement(lineStart, true, true, 0, click)
                }
                return true
            }
        }
    }

    fun showDialogPanel(event: MouseEvent) {
        JBPopupFactory.getInstance().createBalloonBuilder(createAIPanel(this))
            .setFillColor(UIUtil.getPanelBackground())
            .setBorderColor(UIUtil.getFocusedBorderColor())
            .setHideOnAction(false)
            .createBalloon()
            .show(RelativePoint(event.locationOnScreen), Balloon.Position.atRight)
    }

    override fun createConfigurable(settings: DartAISetting): ImmediateConfigurable {
        return AISettingPanel()
    }

    override fun dispose() {
        println("销毁了...")
    }

}

///ai面板
data class AIPanelModel(
    var content: String = "帮我写一个dart函数,功能是复制文本到剪贴板",
    var chats: MutableList<MyAIChatModel> = mutableListOf()
)


///聊天模型
data class MyAIChatModel(
    var content: StringBuilder = StringBuilder(""),
    val isMe: Boolean = false,
    val q: StringBuilder = StringBuilder(""),
    val id: String = ""
)


///聊天列表

@OptIn(BetaOpenAI::class, DelicateCoroutinesApi::class)
fun createAIPanel(parentDisposable: Disposable): DialogPanel {
    var p by lateinitVal<DialogPanel>()
    val alarm = Alarm(parentDisposable)
    var model = AIPanelModel()

    fun initValidation() {
        alarm.addRequest({
            if (p.isModified()) {
                runWriteAction {
                    p.revalidate()
                    p.repaint()
                }
            }
            initValidation()
        }, 1000)
    }


    fun refreshUi() {
        runWriteAction {
            p.revalidate()
            p.repaint()
        }
    }

    fun submit() {
        p.apply()
        model = model.copy(chats = model.chats.apply {
            add(MyAIChatModel(content = java.lang.StringBuilder(model.content), isMe = true))
        })
        refreshUi()
        GlobalScope.launch(Dispatchers.IO) {
            val qJob = async {
                OpenAiUtil.askSimple(model.content).collect { chunk ->
                    val list = model.chats
                    val index = list.indexOfLast { it.id == chunk.id }
                    if (index != -1) {
                        val ele = model.chats[index]
                        val r = chunk.choices.first().delta?.content ?: ""
                        ele.content.append(r)
                    } else {
                        val newChat = MyAIChatModel(
                            content = java.lang.StringBuilder(chunk.choices.first().delta?.content ?: ""),
                            id = chunk.id
                        )
                        model = model.copy(chats = model.chats.apply {
                            add(newChat)
                        })
                    }
                    runWriteAction {
                        p.repaint()
                    }
                }
            }
            qJob.await()
        }
    }

    p = panel {
        row("Question") {
            textArea()
                .columns(40)
                .rows(3)
                .bindText(model::content)
                .gap(RightGap.SMALL)
                .focused()
                .align(Align.FILL).component.apply {
                    minimumSize = Dimension(-1, 160)
                }
        }.enabled(false)
        row("") {
            button("Submit") {
                submit()
            }
            actionsButton(
                DartAiSwitchAction.getInstance(),
                object : MyDumbAwareAction({ "Setting" }) {
                    override fun actionPerformed(e: AnActionEvent) {
                        AiApiKeyConfigDialog(e.project!!).show()
                    }
                }, object : MyDumbAwareAction({ "Help" }) {
                    override fun actionPerformed(e: AnActionEvent) {
                    }
                }, icon = AllIcons.General.Settings
            )
        }.actionButton(object : MyDumbAwareAction("历史记录", "查看历史记录", AllIcons.General.Beta) {
            override fun actionPerformed(e: AnActionEvent) {

            }
        })

        for (item in model.chats) {
            row("Q: ") {
                label(item.content.toString())
            }.visible(item.isMe)
            row("AI:") {
                label(item.content.toString())
            }
        }

    }
    val disposable = Disposer.newDisposable()
    p.registerValidators(disposable)
    Disposer.register(parentDisposable, disposable)

    SwingUtilities.invokeLater {
        initValidation()
    }
    return p
}


class AISettingPanel : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        return BorderLayoutPanel().apply {
            addToTop(JBLabel(""))
        }
    }

}