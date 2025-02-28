package shop.itbug.fluttercheckversionx.actions.freezed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * 给文件中的所有freezed class 添加 sealed
 */
abstract class Freezed3ClassFixAction : AnAction() {

    companion object {

        fun createElement(token: IElementType, project: Project): PsiElement {
            if (token == DartTokenTypes.SEALED) {
                return MyDartPsiElementUtil.createSealedPsiElement(project)
            }
            return MyDartPsiElementUtil.createAbstractPsiElement(project)
        }

        suspend fun createElementByXc(token: IElementType, project: Project): PsiElement {
            if (token == DartTokenTypes.SEALED) {
                return readAction { MyDartPsiElementUtil.createSealedPsiElement(project) }
            }
            return readAction { MyDartPsiElementUtil.createAbstractPsiElement(project) }
        }

        fun fix(element: DartClassDefinitionImpl, newElement: PsiElement, project: Project) {
            val clazz = element.node.findChildByType(DartTokenTypes.CLASS)?.psi ?: return
            WriteCommandAction.runWriteCommandAction(project) {
                element.addBefore(newElement, clazz)
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = psiFile.project
        val dartClassList = PsiTreeUtil.findChildrenOfType(psiFile, DartClassDefinitionImpl::class.java)
        if (dartClassList.isEmpty()) return
        val waitFixClass =
            dartClassList.filter { it.myManagerFun().hasFreezeMetadata() && it.myManagerFun().isFreezed3Class().not() }

        println("修复:${waitFixClass.size}")
        waitFixClass.forEach {
            fix(it, createElement(getInsetDartTypElement(), project), project)
        }
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    abstract fun getInsetDartTypElement(): IElementType
}


//SEALED
class Freezed3ClassFixBySealed : Freezed3ClassFixAction() {
    override fun getInsetDartTypElement(): IElementType {
        return DartTokenTypes.SEALED
    }
}


class Freezed3ClassFixByAbstract : Freezed3ClassFixAction() {

    override fun getInsetDartTypElement(): IElementType {
        return DartTokenTypes.ABSTRACT
    }

}