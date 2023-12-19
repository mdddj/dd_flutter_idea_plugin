package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.exByModifyPsiElementText


///将组件转换成
class StatelessToConsumer : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val psi = e.getData(CommonDataKeys.PSI_ELEMENT)
        val project = e.project
        if (psi is DartComponentNameImpl && psi.parent is DartClassDefinitionImpl
            && (psi.parent as DartClassDefinitionImpl).superclass?.type?.text == "StatelessWidget"
            && project != null
        ) {
            val parent = psi.parent as DartClassDefinitionImpl
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

                            }
                        }
                    }
                }
            }

        }
    }

    override fun update(e: AnActionEvent) {
        val psi = e.getData(CommonDataKeys.PSI_ELEMENT)
        val project = e.project
        e.presentation.isEnabled = psi is DartComponentNameImpl && psi.parent is DartClassDefinitionImpl
                && (psi.parent as DartClassDefinitionImpl).superclass?.type?.text == "StatelessWidget"
                && project != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
