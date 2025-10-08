package shop.itbug.fluttercheckversionx.inlay.yaml

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.endOffset
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.MyActionUtil
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import java.awt.Cursor
import javax.swing.JComponent

class DartPackageSearchDialogInlay : InlayHintsProvider<DartPackageSearchDialogInlay.Settings> {
    override val name: String
        get() = "Add dart package"
    override val key: SettingsKey<Settings>
        get() = SettingsKey("dart.packageSearchDialog")
    override val previewText: String
        get() = """
            dependencies:
              flutter:
                sdk: flutter

        """.trimIndent()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {

        fun isDepsEle(element: PsiElement): Boolean {
            return element.text == "dependencies"
        }

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(
                element: PsiElement,
                editor: Editor,
                sink: InlayHintsSink
            ): Boolean {
                if (MyFileUtil.isFlutterPubspecFile(file) && isDepsEle(element)) {
                    sink.addInlineElement(
                        element.endOffset,
                        false,
                        factory.onClick(
                            factory.withCursorOnHover(
                                factory.roundWithBackground(
                                    factory.seq(
                                        factory.smallScaledIcon(MyIcons.dartPackageIcon),
                                        factory.smallText(" Add package"),
                                    )
                                ), Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            ), MouseButton.Left
                        ) { _, _ ->
                            MyActionUtil.showPubSearchDialog(
                                project = element.project,
                                file as YAMLFile,
                            )
                        }, true
                    )
                }
                return true
            }

        }
    }

    override fun createSettings(): Settings {
        return Settings()
    }

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel { }
            }
        }
    }

    class Settings
}