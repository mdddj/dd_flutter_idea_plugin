package shop.itbug.fluttercheckversionx.inlay.freezed

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.dialog.JsonToFreezedInputDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.manager.DartClassManager
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import java.awt.event.MouseEvent

/**
 * freezed class 操作
 */
class FreezedActionInlay : CodeVisionProviderBase() {

    override fun acceptsFile(file: PsiFile): Boolean {
        return file is DartFile
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        return element is DartClassDefinitionImpl && element.myManagerFun().hasFreezeMetadata() && DartClassManager(
            element
        ).findFreezedMetadata() != null
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        return "Freezed Action"
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        event?.let {
            showFreezedActionMenu(it, element, editor)
        }

    }

    override val name: String
        get() = "Freezed Class Action"
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = emptyList()
    override val id: String
        get() = "FreezedActionInlay"


    //显示操作菜单
    private fun showFreezedActionMenu(mouseEvent: MouseEvent, psiElement: PsiElement, edit: Editor) {
        val popupCreate = JBPopupFactory.getInstance().createActionGroupPopup(
            "Freezed Actions", createFreezedActionGroup(psiElement, mouseEvent, edit), DataContext.EMPTY_CONTEXT,
            JBPopupFactory.ActionSelectionAid.MNEMONICS, true
        )
        popupCreate.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen))
    }

    //操作列表
    private fun createFreezedActionGroup(
        psiElement: PsiElement,
        mouseEvent: MouseEvent,
        edit: Editor
    ): DefaultActionGroup {
        val dartClassElement = psiElement as DartClassDefinitionImpl
        val className = dartClassElement.componentName.name ?: ""
        val dartClassManager = DartClassManager(className, dartClassElement)
        return DefaultActionGroup().apply {
            add(object : MyAction({ "Rename" }) {
                override fun actionPerformed(e: AnActionEvent) {
                    WidgetUtil.getTextEditorPopup(
                        PluginBundle.get("create_a_new_class_name"),
                        className, className,
                        null, { it.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen)) }) {
                        val project = psiElement.project
                        val newClassName = it
                        val newCom: DartComponentNameImpl =
                            MyDartPsiElementUtil.createDartNamePsiElement(newClassName, project)
                        val newDartType = MyDartPsiElementUtil.createDartTypeImplElement("_$newClassName", project)
                        val newFunBody = MyDartPsiElementUtil.createFunBody(
                            "factory ${newClassName}.fromJson(Map<String, dynamic> json) => _$${newClassName}FromJson(json);",
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