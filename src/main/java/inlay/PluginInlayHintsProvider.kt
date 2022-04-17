package inlay

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import icons.MyIcons
import util.MyPsiElementUtil
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*
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
            测试一下preview Text 属性
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