package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentOfType
import com.intellij.psi.util.parents
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLTextUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable
import shop.itbug.fluttercheckversionx.util.CacheUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Rectangle
import javax.xml.crypto.dsig.keyinfo.KeyValue

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
    ): InlayHintsCollector? {

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {


                if (element is YAMLKeyValueImpl && element.parents(false).iterator().hasNext() && element.parents(false).iterator().next().parent is YAMLKeyValueImpl) {
                    val ds = listOf("dependencies", "dev_dependencies", "dependency_overrides")
                    val parentKeyText = (element.parents(false).iterator().next().parent as YAMLKeyValueImpl).keyText
                    if (ds.contains(parentKeyText) && element.keyText != "flutter" && element.keyText != "flutter_test") {
                        val pluginName = MyPsiElementUtil.getPluginNameWithPsi(element)
                        val myFactory = HintsInlayPresentationFactory(factory = factory)
                        if (pluginName.isNotBlank()) {
                            val get = CacheUtil.unredCaChe().asMap()[pluginName]
                            if (get != null && get == pluginName) {

                                sink.addInlineElement(
                                    element.endOffset,
                                    false,
                                    myFactory.simpleText(
                                        "Never used",
                                        "This plug-in package has not been used in the project, it is recommended to delete it to reduce the size of the installation package (此插件包从未使用过,建议删除,可减少安装包大小)"
                                    ),
                                    true
                                )
                            }

                            sink.addBlockElement(
                                 element.textRange.startOffset,
                                relatesToPrecedingText = true,
                                showAbove = true,
                                priority = 1,
                                presentation = myFactory.menuActions(pluginName)

                            )
//                            sink.addInlineElement(element.endOffset, false, myFactory.menuActions(pluginName), false)
                        }
                    }
                }
                return true
            }
        }
    }


}