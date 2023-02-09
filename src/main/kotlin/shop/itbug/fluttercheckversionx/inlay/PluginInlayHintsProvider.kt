package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable
import shop.itbug.fluttercheckversionx.util.isDartPluginElement

class PluginInlayHintsProvider : InlayHintsProvider<PluginInlayHintsProvider.Settings> {


    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("plug.hint.provider")
    }

    data class Settings(
        val show: Boolean = true
    )

    override val key: SettingsKey<Settings>
        get() = KEY
    override val name: String
        get() = "settings.inlay.menus"
    override val previewText: String
        get() = """
dependencies:
  extended_image: ^6.0.2+1
  flutter_easyrefresh: ^2.2.1
        """.trimIndent()

    /**
     * Creates configurable, that immediately applies changes from UI to [settings]
     */
    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }

    override fun createSettings(): Settings {
        return Settings()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (element.isDartPluginElement()) {
                    val pathElement = PsiTreeUtil.findChildOfAnyType(element, YAMLKeyValueImpl::class.java)
//                    pathElement?.let {
//                        if (pathElement.keyText == "path" || pathElement.keyText == "github") {
//                            editor.foldingModel.runBatchFoldingOperation {
//                                editor.foldingModel.addFoldRegion(
//                                    element.startOffset,
//                                    element.endOffset,
//                                    "引入本地插件"
//                                )
//                            }
//                        }
//                    }
                }
                return true
            }
        }
    }


}