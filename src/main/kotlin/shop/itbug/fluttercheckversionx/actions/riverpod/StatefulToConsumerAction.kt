package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartReturnTypeImpl
import com.jetbrains.lang.dart.psi.impl.DartSuperclassImpl
import com.jetbrains.lang.dart.util.DartElementGenerator
import shop.itbug.fluttercheckversionx.constance.MyKeys
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil


private fun AnActionEvent.getEditClassPsi(): DartClassDefinitionImpl? {
    return getData(CommonDataKeys.EDITOR)?.getUserData(MyKeys.DartClassKey)
}

///是否开启
private fun AnActionEvent.isEnableAction(): Boolean {
    val psi = getData(CommonDataKeys.PSI_ELEMENT)
    if (project == null) {
        return false
    }
    val editPsi = getEditClassPsi()
    if (editPsi != null && editPsi.superclass?.type?.text == "StatefulWidget") {
        return true
    }
    return psi is DartComponentNameImpl && psi.parent is DartClassDefinitionImpl
            && (psi.parent as DartClassDefinitionImpl).superclass?.type?.text == "StatefulWidget"
}


///创建类
private fun Project.createSuperclass(superClassName: String = "ConsumerStatefulWidget"): DartSuperclassImpl {
    val str = """
        class Test extends $superClassName{}
    """.trimIndent()
    val createDummyFile = DartElementGenerator.createDummyFile(this, str)
    return PsiTreeUtil.findChildOfType(createDummyFile, DartSuperclassImpl::class.java)!!
}


///创建返回值
private fun Project.createReturnType(name: String): DartReturnTypeImpl {
    val str = """
        ConsumerState<$name> fun(){}
    """.trimIndent()
    val file = DartElementGenerator.createDummyFile(this, str)
    return PsiTreeUtil.findChildOfType(file, DartReturnTypeImpl::class.java)!!
}


/// stf
class StatefulToConsumerAction : AnAction() {


    override fun actionPerformed(e: AnActionEvent) {


        val psi = e.getData(CommonDataKeys.EDITOR)?.getUserData(MyKeys.DartClassKey)
        val isClass = psi is DartClassDefinitionImpl
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val dartClassName =
            if (isClass) psi.componentName else e.getData(CommonDataKeys.PSI_ELEMENT) as DartComponentNameImpl
        val classDefinition =
            if (isClass) psi else dartClassName.parent as DartClassDefinitionImpl


        e.project?.let { project ->
            val className = dartClassName.name
            WriteCommandAction.runWriteCommandAction(project) {
                classDefinition.superclass?.replace(project.createSuperclass()) //1.替换继承
            }


            //2.替换函数
            val methods = classDefinition.classBody?.classMembers?.methodDeclarationList ?: emptyList()
            if (methods.isNotEmpty()) {
                val findCreateStateMethod = methods.find { it.componentName?.name == "createState" }
                if (findCreateStateMethod != null) {
                    val newReturnType = project.createReturnType("$className")
                    WriteCommandAction.runWriteCommandAction(project) {
                        findCreateStateMethod.returnType?.replace(newReturnType)
                    }

                }
            }

            //3.替换 state
            val dartFile = psiFile as DartFile
            val dartClassList = PsiTreeUtil.findChildrenOfType(dartFile, DartClassDefinitionImpl::class.java)
            val findStateClass = dartClassList.find { it.superclass?.type?.text == "State<${className}>" }
            WriteCommandAction.runWriteCommandAction(project) {
                findStateClass?.superclass?.replace(project.createSuperclass(superClassName = "ConsumerState<$className>"))
            }
            //添加导入语句
            MyDartPsiElementUtil.addRiverpodHookImport(psiFile, project)
        }


    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.isEnableAction()
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}
