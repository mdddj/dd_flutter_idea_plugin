package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper
import java.io.File
import javax.swing.JComponent

/**
 * 检测dart文件中的字符串,或者字符串引用,来显示一个图片
 */
class DartStringIconShowInlay : InlayHintsProvider<NoSettings> {
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("dart.string.icon.showInlay")
    override val name: String
        get() = "Dart Assets Image Show Inlay"
    override val previewText: String?
        get() = """"""

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
    }

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun getCollectorFor(
        file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink
    ): InlayHintsCollector? {
        return object : FactoryInlayHintsCollector(editor) {
            val myInlayFactory = HintsInlayPresentationFactory(factory)
            override fun collect(
                element: PsiElement, editor: Editor, sink: InlayHintsSink
            ): Boolean {
                val setting = PluginConfig.getInstance(element.project).state
                if (setting.showAssetsIconInEditor.not()) {
                    return true
                }
                val file = DartPsiElementHelper.checkHasFile(element) ?: return true
                val inlay = myInlayFactory.getImageWithPath(file.full, file.basePath, setting) ?: return true
                sink.addInlineElement(element.textRange.startOffset, false, inlay, false)
                return true
            }
        }
    }

    data class FileResult(
        val file: File, val basePath: String, val full: String
    )

}