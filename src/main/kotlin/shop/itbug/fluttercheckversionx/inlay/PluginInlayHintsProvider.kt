package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.components.JBLabel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import javax.swing.JComponent

class PluginInlayHintsProvider : InlayHintsProvider<PluginInlayHintsProvider> {


    override val key: SettingsKey<PluginInlayHintsProvider>
        get() = SettingsKey(PluginBundle.get("flutterX-Dart-Plugin-Ignore"))
    override val name: String
        get() = PluginBundle.get("flutterX-Dart-Plugin-Ignore")
    override val previewText: String
        get() = PluginBundle.get("flutterX-Dart-Plugin-Ignore")

    override fun createSettings(): PluginInlayHintsProvider {
        TODO("Not yet implemented")
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: PluginInlayHintsProvider,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : InlayHintsCollector {

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
//                val ex = YamlExtends(element)
//                val config = DartPluginIgnoreConfig.getInstance(file.project)
//                if (ex.isDartPluginElement() && config.isIg(ex.getDartPluginNameAndVersion()?.name ?: "") ) {
//                    sink.addPresentation(InlineInlayPosition(element.endOffset, true, 0), emptyList(), "", true) {
//                        this.text(PluginBundle.get("flutterX-Dart-Plugin-Ignore")) //提示忽略检测
//                    }
//                    sink.addInlineElement(element.endOffset)
//                }
                return true
            }

        }
    }

    override fun createConfigurable(settings: PluginInlayHintsProvider): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JBLabel("none")
            }

        }
    }

}