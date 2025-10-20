package shop.itbug.fluttercheckversionx.inlay.yaml

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.YamlExtends
import java.awt.Cursor
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
                val file = tool.getPathResolvePath()
                if (file != null) {
                    sink.addInlineElement(
                        element.textRange.endOffset,
                        false,
                        factory.inset(
                            factory.roundWithBackground(
                            factory.onClick(
                                factory.withCursorOnHover(
                                    factory.seq(
                                        factory.withTooltip("Open in Explorer", factory.smallText("${file.path}"))
                                    ), Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                ), MouseButton.Left
                            ) { _, _ ->
                                BrowserUtil.browse(file)
                            }
                        ), left = 5),
                        false
                    )
                    val pubspecFilePath =  file.resolve("pubspec.yaml")
                    if (pubspecFilePath.exists() && pubspecFilePath.isFile) {
                        sink.addInlineElement(
                            element.textRange.endOffset,
                            false,
                            factory.inset(
                                factory.roundWithBackground(
                                    factory.onClick(
                                        factory.withCursorOnHover(
                                            factory.seq(
                                                factory.smallScaledIcon(MyIcons.androidStudio),
                                                factory.smallText("Open in Android Studio")
                                            ), Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                        ), MouseButton.Left
                                    ) { _, _ ->
                                        //打开文件夹
                                        ProjectManager.getInstance().loadAndOpenProject(file.path)
                                    }
                                ), left = 5),
                            false
                        )
                    }
                }
                return true
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