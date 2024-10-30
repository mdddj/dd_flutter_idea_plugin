package shop.itbug.fluttercheckversionx.actions.riverpod


import com.intellij.codeInsight.hints.*
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.constance.MyKeys
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.services.PubspecService
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.event.MouseEvent
import javax.swing.JComponent


private fun PsiElement.needHandler(): Boolean {
    val setting = PluginConfig.getState(project)
    val config: List<String> = MyPsiElementUtil.getAllPlugins(project)
    val hasRiverpod = PubspecService.getInstance(project).hasRiverpod()
    val element = this
    return setting.showRiverpodInlay && element is DartClassDefinitionImpl && hasRiverpod && (element.superclass?.type?.text == "StatelessWidget" || element.superclass?.type?.text == "StatefulWidget")
}

class PsiElementEditorPopupMenuInlay : InlayHintsProvider<NoSettings> {

    private fun showPopup(mouseEvent: MouseEvent, editor: Editor, element: PsiElement) {
        val group = ActionManager.getInstance().getAction("WidgetToRiverpod") as DefaultActionGroup
        editor.putUserData(MyKeys.DartClassKey, element as DartClassDefinitionImpl)
        val context = DataManager.getInstance().getDataContext(editor.component)
        val popupCreate = JBPopupFactory.getInstance().createActionGroupPopup(
            "Riverpod To", group, context,
            JBPopupFactory.ActionSelectionAid.MNEMONICS, true
        )
        popupCreate.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen))
    }

    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("RiverpodClassToolsSettingKey")
    override val name: String
        get() = "FlutterXRiverpodClassTool"
    override val previewText: String?
        get() = null

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            val myFactory = HintsInlayPresentationFactory(factory)
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                if (element.needHandler()) {
                    sink.addBlockElement(
                        element.textRange.startOffset,
                        true,
                        showAbove = true,
                        priority = 1,
                        presentation = myFactory.iconText(
                            AllIcons.General.ChevronDown, "Riverpod Tool", false,
                            handle = { mouseEvent, _ ->
                                run {
                                    showPopup(mouseEvent, editor, element)
                                }
                            },
                        )
                    )
                }
                return true
            }
        }
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
    }

}