package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.psi.impl.DartMethodDeclarationImpl
import java.awt.Graphics
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

/**
 * dart ai 助手
 */
class DartCodeAIInlay : InlayHintsProvider<DartAISetting> {
    override val key: SettingsKey<DartAISetting>
        get() = SettingsKey(name)
    override val name: String
        get() = "Flutter-Check-AI-Setting"
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
        return object : InlayHintsCollector {


            ///检索组件
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

                //如果是一个psi组件,将添加一个ai问询的layout
                if (element is DartMethodDeclarationImpl) {
                    println("进来了:${element.text}")
                    ApplicationManager.getApplication().invokeLater {
                        val line = editor.document.getLineNumber(element.startOffset)
                        val s = editor.document.getLineStartOffset(line)
                        val e = editor.document.getLineEndOffset(line)
                        editor.inlayModel.addBlockElement(e, true, true, 1, CustomRender())
                    }
                }

                return true
            }
        }
    }

    override fun createConfigurable(settings: DartAISetting): ImmediateConfigurable {
        return AISettingPanel()
    }


}


class CustomRender : EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return 200
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        super.paint(inlay, g, targetRegion, textAttributes)
        g.drawString("呼叫AI", targetRegion.x, targetRegion.y)
    }

}

class AISettingPanel : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        return BorderLayoutPanel().apply {
            addToTop(JBLabel("AI设置"))
        }
    }

}