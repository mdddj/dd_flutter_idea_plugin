package shop.itbug.fluttercheckversionx.inlay.freezed

import com.intellij.codeInsight.hints.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.manager.DartClassManager
import shop.itbug.fluttercheckversionx.util.dart.DartClassUtil
import shop.itbug.fluttercheckversionx.util.exByModifyAllPsiElementText
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import java.awt.event.MouseEvent
import javax.swing.JComponent


data class FreezedInlaySetting(var show: Boolean)

class FreezedInlay : InlayHintsProvider<FreezedInlaySetting> {
    override val key: SettingsKey<FreezedInlaySetting>
        get() = SettingsKey("freezed inlay")
    override val name: String
        get() = "FreezedInlay"
    override val previewText: String
        get() = "@freezed" +
                "class Test {}"

    override fun createSettings(): FreezedInlaySetting {
        return FreezedInlaySetting(true)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: FreezedInlaySetting,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return FreezedInlayCollector(editor)
    }

    override fun createConfigurable(settings: FreezedInlaySetting): ImmediateConfigurable {
        return FreezedInlayPanel()
    }

}


class FreezedInlayCollector(val edit: Editor) : FactoryInlayHintsCollector(edit) {

    private val inlayFactory = HintsInlayPresentationFactory(factory)
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {

        val isFreezedClass =
            DartClassUtil.hasMetadata(element, "freezed") || DartClassUtil.hasMetadata(element, "Freezed")
        if (isFreezedClass) {
//            val lineStart = editor.getLineStart(element)
//            val inlayPresentation =
//                inlayFactory.iconText(AllIcons.General.ChevronDown, "Freezed Actions ", false) { mouseEvent, point ->
//                    showFreezedActionMenu(mouseEvent, point, element)
//                }
//            sink.addBlockElement(lineStart, true, true, 0, inlayPresentation)
            val manager = DartClassManager(psiElement = element as DartClassDefinitionImpl)
            val freezedElement = manager.findMataDataByText("freezed") ?: manager.findMataDataByText("Freezed")
            freezedElement?.let {
                sink.addInlineElement(
                    it.endOffset,
                    true,
                    inlayFactory.iconRoundClick(AllIcons.General.ChevronDown) { mouseEvent, _ ->
                        showFreezedActionMenu(
                            mouseEvent,
                            element
                        )
                    },
                    true
                )
            }


        }
        return true
    }

    //显示操作菜单
    private fun showFreezedActionMenu(mouseEvent: MouseEvent, psiElement: PsiElement) {


        val popupCreate = JBPopupFactory.getInstance().createActionGroupPopup(
            "Freezed Actions", createFreezedActionGroup(psiElement, mouseEvent), DataContext.EMPTY_CONTEXT,
            JBPopupFactory.ActionSelectionAid.MNEMONICS, true
        )
        popupCreate.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen))
    }

    //操作列表
    private fun createFreezedActionGroup(psiElement: PsiElement, mouseEvent: MouseEvent): DefaultActionGroup {
        val dartClassElement = psiElement as DartClassDefinitionImpl
        val className = dartClassElement.componentName.name ?: ""
        val dartClassManager = DartClassManager(className, dartClassElement)
        return DefaultActionGroup().apply {
            add(object : MyAction({ "Rename" }) {
                override fun actionPerformed(e: AnActionEvent) {

                    WidgetUtil.getTextEditorPopup(
                        "Enter a new Class Name",
                        className,
                        { it.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen)) }) {
                        dartClassElement.exByModifyAllPsiElementText(it, className)
                        dartClassElement.exByModifyAllPsiElementText("_$$it", "_$$className")
                        dartClassElement.exByModifyAllPsiElementText("_$it", "_$className")
                        dartClassElement.exByModifyAllPsiElementText("_$$it" + "FromJson", "_$$className" + "FromJson")
                    }

                }
            })



            add(object : MyAction({ "Create FromJson Function" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    dartClassManager.addFreezedFromJsonConstructor(ifExists = {
                        val fromJsonEle = (it as DartFactoryConstructorDeclarationImpl).componentNameList[1]
                        dartClassManager.showHints(edit, fromJsonEle, "Already exists")
                    })
                }
            })
            add(object : MyAction({ "Add HiveField annotation" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    e.project?.toast("This feature will be released in version 3.6.0")
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = false
                    super.update(e)
                }

                override fun getActionUpdateThread(): ActionUpdateThread {
                    return ActionUpdateThread.BGT
                }
            })

            addSeparator()

            add(object : MyAction({ "Add .freezed part" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    dartClassManager.addPartForSuffix("freezed") {
                        dartClassManager.showHints(edit, it, "Already exists")
                    }
                }
            })

            add(object : MyAction({ "Add .g part" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    dartClassManager.addPartForSuffix("g") {
                        dartClassManager.showHints(edit, it, "Already exists")
                    }
                }
            })

            addSeparator()

            add(object : MyAction({ "Json to Freezed" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    JsonToFreezedInputDialog(psiElement.project).show()
                }
            })
            add(object : MyAction({ "Run build runner" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    TerminalToolWindowManager.getInstance(psiElement.project)
                        .createLocalShellWidget(psiElement.project.basePath, "FlutterCheckVersionX")
                        .executeCommand("flutter pub run build_runner build")
                }
            })

        }
    }
}

class FreezedInlayPanel : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        return panel { }
    }

}