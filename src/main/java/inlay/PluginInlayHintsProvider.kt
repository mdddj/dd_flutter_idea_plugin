package inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import util.CacheUtil
import util.MyPsiElementUtil
import javax.swing.JComponent
import javax.swing.JPanel

class PluginInlayHintsProvider : InlayHintsProvider<PluginInlayHintsProvider.Settings> {



    companion object {
        private val KEY: SettingsKey<Settings> = SettingsKey("plug.hint.provider")
    }

    data class Settings (
        val show: Boolean = true
    )

    override val key: SettingsKey<Settings>
        get() = KEY
    override val name: String
        get() = "settings.inlay.menus"
    override val previewText: String?
        get() = """
dependencies:
  extended_image: ^6.0.2+1
  flutter_easyrefresh: ^2.2.1
        """.trimIndent()

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
                val pluginName = MyPsiElementUtil.getPluginNameWithPsi(element)
                val myFactory = HintsInlayPresentationFactory(factory = factory)
                if(pluginName.isNotBlank()){
                    val get = CacheUtil.unredCaChe().asMap()[pluginName]
                    if(get!=null && get == pluginName){
                        sink.addInlineElement(element.endOffset,false,myFactory.simpleText("从未使用","此插件包在项目中没有使用过,建议删除,可减少安装包体积"),true)
                    }

                    sink.addInlineElement(element.textOffset,false,myFactory.menuActions(element,pluginName),false)




                }
                return true
            }
        }
    }

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JPanel()
            }

            override val mainCheckboxText: String
                get() = "111"
        }
    }

    override val group: InlayGroup
        get() = InlayGroup.TYPES_GROUP







}