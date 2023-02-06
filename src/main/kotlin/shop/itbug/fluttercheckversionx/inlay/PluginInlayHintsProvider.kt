package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable
import shop.itbug.fluttercheckversionx.util.getPluginName
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
    ): InlayHintsCollector? {

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

                if(element.isDartPluginElement()){
                    val pluginName = element.getPluginName()
                    HintsInlayPresentationFactory(factory = factory)
                    if (pluginName.isNotBlank()) {
//                        val get = CacheUtil.unredCaChe().asMap()[pluginName]
//                        if (get != null && get == pluginName) {

                            ///TODO 检测插件是否在项目中使用过
//                            sink.addInlineElement(
//                                element.endOffset,
//                                false,
//                                myFactory.simpleText(
//                                    "Never used",
//                                    "This plug-in package has not been used in the project, it is recommended to delete it to reduce the size of the installation package (此插件包从未使用过,建议删除,可减少安装包大小)"
//                                ),
//                                true
//                            )
//                        }
                    }
                }

                return true
            }
        }
    }


}