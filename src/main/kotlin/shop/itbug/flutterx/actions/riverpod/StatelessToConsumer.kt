package shop.itbug.flutterx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import shop.itbug.flutterx.constance.MyKeys
import shop.itbug.flutterx.util.MyDartPsiElementUtil
import shop.itbug.flutterx.util.exByModifyPsiElementText


private fun AnActionEvent.getEditorClass(): DartClassDefinitionImpl? {
    return getData(CommonDataKeys.EDITOR)?.getUserData(MyKeys.DartClassKey)
}

private fun AnActionEvent.isEnable(): Boolean {
    if (project == null) {
        return false
    }
    val psi = getData(CommonDataKeys.PSI_ELEMENT)
    val editPsi = getEditorClass()
    if (editPsi != null && editPsi.superclass?.type?.text == "StatelessWidget") {
        return true
    }
    if (psi is DartComponentNameImpl && psi.parent is DartClassDefinitionImpl
        && (psi.parent as DartClassDefinitionImpl).superclass?.type?.text == "StatelessWidget"
    ) {
        return true
    }
    return false
}


///将组件转换成
class StatelessToConsumer : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val classElement = e.getEditorClass()
        val namePsi = classElement?.componentName ?: e.getData(CommonDataKeys.PSI_ELEMENT)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return


        val project = e.project
        if (namePsi is DartComponentNameImpl && namePsi.parent is DartClassDefinitionImpl
            && (namePsi.parent as DartClassDefinitionImpl).superclass?.type?.text == "StatelessWidget"
            && project != null
        ) {
            val parent = namePsi.parent as DartClassDefinitionImpl
            parent.classBody?.classMembers?.let { members ->
                val methods = members.methodDeclarationList
                methods.forEach { method ->
                    method.componentName?.name?.let { methodName ->
                        run {
                            if (methodName == "build") {
                                ///添加一个
                                val newPsi = MyDartPsiElementUtil.getWidgetRefParam(project)
                                val params = method.formalParameterList.normalFormalParameterList
                                if (params.find { it.simpleFormalParameter?.type?.text == "WidgetRef" } == null) {
                                    WriteCommandAction.runWriteCommandAction(project) {
                                        val context =
                                            params.find { it.simpleFormalParameter?.type?.text == "BuildContext" }
                                        context?.let { _ ->
                                            method.formalParameterList.replace(newPsi)
                                        }
                                        ///替换继承
                                        parent.superclass?.type?.exByModifyPsiElementText("ConsumerWidget")
                                    }

                                } else {
                                    println("已经有了")
                                }

                                //添加导入语句
                                MyDartPsiElementUtil.addRiverpodHookImport(psiFile, project)
                            }
                        }
                    }
                }
            }

        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.isEnable()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
