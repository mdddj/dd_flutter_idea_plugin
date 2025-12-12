package shop.itbug.flutterx.actions.internal

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import com.intellij.util.lateinitVal
import com.intellij.util.ui.JBUI
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import kotlinx.coroutines.runBlocking
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.services.ArbFile
import shop.itbug.flutterx.services.FlutterL10nService
import shop.itbug.flutterx.services.hasKey
import shop.itbug.flutterx.services.insetNewKey
import shop.itbug.flutterx.util.getRelativePoint
import shop.itbug.flutterx.util.string
import java.awt.Dimension
import javax.swing.*

class L10nKeyExtractIntentionAction : PsiElementBaseIntentionAction(), Iconable {
    lateinit var balloon: Balloon
    override fun invoke(
        project: Project, editor: Editor?, element: PsiElement
    ) {
        editor ?: return
        if (editor is ImaginaryEditor) return
        val text = (element.parent as? DartStringLiteralExpressionImpl)?.string ?: return
        println("提取为l10n key")
        ///获取编辑器当前指针 Point
        val point = element.getRelativePoint(editor)
        balloon = JBPopupFactory.getInstance().createBalloonBuilder(popupDialogPanel(project, text) { key, arbFile ->
            println("key si $key  arb file is $arbFile  text is $text")
            balloon.hide(true)
            ApplicationManager.getApplication().invokeLater {
                FlutterL10nService.getInstance(project).runWriteThread {
                    arbFile.insetNewKey(key, text)
                }
            }
        })
            .setContentInsets(JBUI.insets(32))
            .setFillColor(JBColor.PanelBackground)
            .setBorderColor(JBColor.PanelBackground)
            .createBalloon()

        balloon.show(
            point, Balloon.Position.above
        )

    }

    override fun isAvailable(
        project: Project, editor: Editor?, element: PsiElement
    ): Boolean {
        val sp = element.parent as? DartStringLiteralExpressionImpl ?: return false
        return sp.string.isNullOrBlank().not()
    }

    override fun getFamilyName(): @IntentionFamilyName String {
        return "Extract L10n key"
    }

    override fun getText(): @IntentionName String {
        return familyName
    }

    override fun getIcon(flags: Int): Icon {
        return MyIcons.flutter
    }


}


///ui
private fun popupDialogPanel(
    project: Project,
    text: String,
    doOk: (key: String, arbFile: ArbFile) -> Unit
): DialogPanel {
    val service = FlutterL10nService.getInstance(project)
    val arbFiles = service.arbFiles
    val comboBoxModel = DefaultComboBoxModel(arbFiles.toTypedArray())
    var keyText = ""
    var saveTo: ArbFile? = null
    var myPanel by lateinitVal<DialogPanel>()
    var previewField by lateinitVal<Cell<JEditorPane>>()
    var okButton by lateinitVal<Cell<JButton>>()

    myPanel = panel {
        row("Text") {
            label(text)
        }
        row("key") {
            textField().bindText({ keyText }, { keyText = it }).align(Align.FILL).validationOnInput {
                println("进来了验证：${it.text}")
                if (saveTo == null) return@validationOnInput null
                val hasKey = runBlocking { saveTo!!.hasKey(keyText) }
                println("has key: $hasKey")
                if (hasKey) return@validationOnInput this.error("Already exists")
                return@validationOnInput null
            }
        }
        row("Save to") {
            comboBox(comboBoxModel).bindItem({ saveTo }, {
                saveTo = it
            })
                .align(Align.FILL)
        }
        row("Preview") {
            previewField = comment("").align(Align.FILL)

                .apply {
                    component.text = "\"$keyText\":\"$text\""
                }
        }
        row {
            okButton = button("Ok") {
                myPanel.apply()
                if (saveTo != null) {
                    doOk.invoke(keyText, saveTo!!)
                }
            }.enabled(saveTo != null).align(AlignX.RIGHT)
        }
    }
    myPanel.isOpaque = true
    myPanel.preferredSize = Dimension(400, myPanel.preferredSize.height + 44)
    myPanel.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)

    val newDisposable = Disposer.newDisposable(service)
    val alarm = Alarm(newDisposable)
    myPanel.registerValidators(newDisposable)

    fun previewGenerate() {
        alarm.addRequest({
            if (myPanel.isModified()) {
                myPanel.apply()
                //生成预览
                previewField.component.text = "\"$keyText\":\"$text\""
                okButton.component.isEnabled = saveTo != null && keyText.isNotEmpty()
            }
            previewGenerate()
        }, 500)
    }

    SwingUtilities.invokeLater {
        previewGenerate()
    }


    return myPanel
}