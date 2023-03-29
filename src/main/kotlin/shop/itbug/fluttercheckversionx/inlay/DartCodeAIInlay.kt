package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.psi.impl.DartMethodDeclarationImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.swing.JComponent


data class DartAIConfig(var showInEditor: Boolean = true)

@State(name = "DartAIConfig", storages = [Storage("DartAIConfig.xml")])
class DartAISetting private constructor() : PersistentStateComponent<DartAIConfig> {
    var st = DartAIConfig()
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


class DartCodeAIInlay : InlayHintsProvider<DartAISetting> {

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
                HintsInlayPresentationFactory(factory)


                val offset = element.textRange.startOffset
                val line = editor.document.getLineNumber(offset)
                val lineStart = editor.document.getLineStartOffset(line)
                val indent = offset - lineStart //缩进
                val indentText = StringUtil.repeat(" ", indent)
                if (element is DartMethodDeclarationImpl) {
                    val text = factory.text("$indentText ")
                    val icon = factory.smallScaledIcon(MyIcons.openai)

                    val ai = factory.smallText(" 求助AI")
                    val newF = factory.seq(text,factory.roundWithBackgroundAndSmallInset(factory.seq(icon, ai)) )
                    sink.addBlockElement(lineStart, true, true, 0, CustomRender(newF))
                }
                return true
            }
        }
    }

    override fun createConfigurable(settings: DartAISetting): ImmediateConfigurable {
        return AISettingPanel()
    }

}


class CustomRender(val text: InlayPresentation) : InlayPresentation {


    override val height: Int
        get() = text.height
    override val width: Int
        get() = text.width

    override fun addListener(listener: PresentationListener) {
        text.addListener(listener)
    }

    override fun fireContentChanged(area: Rectangle) {
        text.fireContentChanged(area)
    }

    override fun fireSizeChanged(previous: Dimension, current: Dimension) {
        text.fireSizeChanged(previous, current)
    }

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        text.paint(g, attributes)
    }

    override fun removeListener(listener: PresentationListener) {
        text.removeListener(listener)
    }

    override fun toString(): String {
        return "11"
    }

}

class AISettingPanel : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        return BorderLayoutPanel().apply {
            addToTop(JBLabel(""))
        }
    }

}