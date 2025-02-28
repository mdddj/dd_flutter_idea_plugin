package shop.itbug.fluttercheckversionx.actions.freezed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
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

    override fun actionPerformed(e: AnActionEvent) {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val project = e.getData(CommonDataKeys.PROJECT) ?: return
        val dartClassList = PsiTreeUtil.findChildrenOfType(psiFile, DartClassDefinitionImpl::class.java)
        if (dartClassList.isEmpty()) return
        val waitFixClass =
            dartClassList.filter { it.myManagerFun().hasFreezeMetadata() && it.myManagerFun().isFreezed3Class().not() }

        println("修复:${waitFixClass.size}")
        waitFixClass.forEach {
            fix(project, it)
        }
    }

    // 创建 psi节点
    fun fix(project: Project, element: DartClassDefinitionImpl) {
        val clazz = element.node.findChildByType(DartTokenTypes.CLASS)?.psi ?: return
        var newElement: PsiElement? = null
        var token = getInsetDartTypElement()
        if (token == DartTokenTypes.SEALED) {
            newElement = MyDartPsiElementUtil.createSealedPsiElement(project)
        } else if (token == DartTokenTypes.ABSTRACT) {
            newElement = MyDartPsiElementUtil.createAbstractPsiElement(project)
        }
        WriteCommandAction.runWriteCommandAction(project) {

            if (newElement != null) {
                element.addBefore(newElement, clazz)
            }
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