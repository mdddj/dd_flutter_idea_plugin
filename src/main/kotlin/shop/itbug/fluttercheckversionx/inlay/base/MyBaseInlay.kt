package shop.itbug.fluttercheckversionx.inlay.base

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import javax.swing.JComponent

data class MyBaseInlayModel(
    val psiFile: PsiFile,
    val editor: Editor,
    val settings: PluginSetting,
    val sink: InlayHintsSink,
)

abstract class MyBaseInlay(private val inlayName: String) : InlayHintsProvider<PluginSetting> {
    override val key: SettingsKey<PluginSetting>
        get() = SettingsKey(inlayName)
    override val name: String
        get() = inlayName
    override val previewText: String?
        get() = getMyPreviewText()

    override fun createSettings(): PluginSetting {
        return PluginConfig.getState()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: PluginSetting,
        sink: InlayHintsSink
    ): InlayHintsCollector? {


        return object : FactoryInlayHintsCollector(editor) {
            private val myFactory = HintsInlayPresentationFactory(factory)
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (needHandle(element, settings, editor)) {
                    handle(
                        element, myFactory, MyBaseInlayModel(
                            file,
                            editor,
                            settings,
                            sink,
                        )
                    )
                }
                return true
            }
        }
    }

    override fun createConfigurable(settings: PluginSetting): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return BorderLayoutPanel()
            }
        }
    }

    abstract fun needHandle(element: PsiElement, setting: PluginSetting, editor: Editor): Boolean

    abstract fun handle(element: PsiElement, myFactory: HintsInlayPresentationFactory, model: MyBaseInlayModel)

    private fun getMyPreviewText(): String {
        return ""
    }

}