package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.util.getDartClassDefinition

private fun AnActionEvent.ele( ): PsiElement? {
    return  getData(CommonDataKeys.PSI_ELEMENT)
}
/**
 * 将类转移到其他文件中去
 */
class MoveClassToOtherFile : MyAction({""}) {
    override fun actionPerformed(e: AnActionEvent) {

    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project!=null && e.ele() != null &&  e.getDartClassDefinition()!=null
        super.update(e)
    }



    companion object {

        ///操作实例
        val instance : AnAction get() = ActionManager.getInstance().getAction("MoveClassToOtherFile")
    }

}