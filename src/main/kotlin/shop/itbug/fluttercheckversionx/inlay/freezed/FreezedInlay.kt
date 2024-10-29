package shop.itbug.fluttercheckversionx.inlay.freezed

import com.intellij.codeInsight.hints.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.manager.DartClassManager
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import java.awt.event.MouseEvent
import javax.swing.JComponent


class FreezedInlay : InlayHintsProvider<DoxListeningSetting> {
    override val key: SettingsKey<DoxListeningSetting>
        get() = SettingsKey("freezed inlay")
    override val name: String
        get() = "FreezedInlay"
    override val previewText: String
        get() = "@freezed\n" +
                "class Test {}"

    override fun createSettings(): DoxListeningSetting {
        return DioListingUiConfig.setting
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: DoxListeningSetting,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return FreezedInlayCollector(editor)
    }

    override fun createConfigurable(settings: DoxListeningSetting): ImmediateConfigurable {
        return FreezedInlayPanel()
    }

}


class FreezedInlayCollector(val edit: Editor) : FactoryInlayHintsCollector(edit) {

    private val inlayFactory = HintsInlayPresentationFactory(factory)
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val isFreezedClass = element is DartClassDefinitionImpl && element.myManagerFun().hasFreezeMetadata()
        if (isFreezedClass) {
            val manager = DartClassManager(psiElement = element)
            val freezedElement = manager.findFreezedMetadata()
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
                        PluginBundle.get("create_a_new_class_name"),
                        className,
                        { it.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen)) }) {
                        val project = psiElement.project
                        val newClassName = it
                        val newCom: DartComponentNameImpl =
                            MyDartPsiElementUtil.createDartNamePsiElement(newClassName, project)
                        val newDartType = MyDartPsiElementUtil.createDartTypeImplElement("_$newClassName", project)
                        val newFunBody = MyDartPsiElementUtil.createFunBody(
                            "factory ${newClassName}.fromJson(Map<String, dynamic> json) => _\$${newClassName}FromJson(json);",
                            project
                        )
                        val newMixin = MyDartPsiElementUtil.createMixin("_$${newClassName}", project)

                        MyPsiElementUtil.findAllMatchingElements(dartClassElement) { text: String, psiElement: PsiElement ->
                            run {

                                //替换基础的类名
                                if (text == className && psiElement is DartComponentNameImpl) {
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        psiElement.replace(newCom)
                                    }
                                }

                                //替换末尾的类名
                                if (psiElement is DartFactoryConstructorDeclarationImpl) {
                                    val lastType = psiElement.lastChild.prevSibling
                                    if (lastType is DartTypeImpl) {
                                        WriteCommandAction.runWriteCommandAction(project) {
                                            lastType.replace(newDartType)
                                        }
                                    }
                                }

                                //替换fromJson
                                if (psiElement is DartFactoryConstructorDeclarationImpl && psiElement.text.contains("fromJson")) {
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        psiElement.replace(newFunBody)
                                    }
                                }

                                //替换mixin
                                if (psiElement is DartTypeListImpl && text == "_$$className") {
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        psiElement.replace(newMixin)
                                    }
                                }
                                true
                            }
                        }
//                        replaceTextInSubElement(dartClassElement, DartNamedElementImpl::class.java, className, newCom)
//                        dartClassElement.exByModifyAllPsiElementText("_$$it", "_$$className")
//                        dartClassElement.exByModifyAllPsiElementText("_$it", "_$className")
//                        dartClassElement.exByModifyAllPsiElementText("_$$it" + "FromJson", "_$$className" + "FromJson")
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
                    RunUtil.runCommand(psiElement.project, "FlutterX run build", "flutter pub run build_runner build")
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