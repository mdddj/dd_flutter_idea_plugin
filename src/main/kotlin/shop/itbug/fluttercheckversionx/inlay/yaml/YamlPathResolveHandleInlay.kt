package shop.itbug.fluttercheckversionx.inlay.yaml

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.YamlExtends
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JComponent

class YamlPathResolveHandleInlay : InlayHintsProvider<NoSettings> {
    override val name: String
        get() = "YamlPathResolveHandleInlay"
    override val key: SettingsKey<NoSettings>
        get() = SettingsKey("yamlPathResolveHandleInlay")
    override val previewText: String
        get() = """"""

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                val tool = YamlExtends(element)
                val findFile = tool.getPathResolvePath()
                if (findFile != null) {




                    sink.addInlineElement(
                        element.textRange.endOffset,
                        false,
                        factory.inset(
                            factory.roundWithBackground(
                            factory.onClick(
                                factory.withCursorOnHover(
                                    factory.seq(
                                        factory.withTooltip("Open in ...", factory.smallText("${findFile.path}")),
                                    ), Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                ), MouseButton.Left
                            ) { event , point ->
                                showOpenInPopup(event,point,editor,findFile)
                            }
                        ), left = 5),
                        false
                    )

                    sink.addInlineElement(
                        element.textRange.endOffset,
                        false,
                        factory.inset(
                            factory.roundWithBackground(
                                factory.onClick(
                                    factory.withCursorOnHover(
                                        factory.seq(
                                            factory.smallScaledIcon(MyIcons.moreHorizontal),
                                        ), Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    ), MouseButton.Left
                                ) { event , point ->
                                    showOpenInPopup(event,point,editor,findFile)
                                }
                            ), left = 5),
                        false
                    )

//                    val pubspecFilePath =  file.resolve("pubspec.yaml")
//                    if (pubspecFilePath.exists() && pubspecFilePath.isFile) {

//                    }
                }
                return true
            }

            fun showOpenInPopup(
                event: MouseEvent,
                point: Point,
                editor: Editor,
                file: File
            ) {
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return
                val actionGroup = ActionManager.getInstance().getAction("FlutterXOpenInAction") as DefaultActionGroup
                val context = SimpleDataContext.getProjectContext(editor.project!!)
                val newContext = SimpleDataContext.getSimpleContext(CommonDataKeys.VIRTUAL_FILE,virtualFile,context)
                val popupCreate = JBPopupFactory.getInstance().createActionGroupPopup(
                    "Open in ...", actionGroup, newContext,
                    JBPopupFactory.ActionSelectionAid.MNEMONICS, true
                )
                popupCreate.show(RelativePoint.fromScreen(event.locationOnScreen))
            }

        }
    }

    override fun createSettings(): NoSettings {
        return NoSettings()
    }

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
    }
}