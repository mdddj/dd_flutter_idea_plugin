package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.util.getDartClassDefinition
import javax.swing.JComponent

private fun AnActionEvent.ele(): PsiElement? {
    return getData(CommonDataKeys.PSI_ELEMENT)
}

/**
 * 将类转移到其他文件中去
 */
class MoveClassToOtherFile : MyAction({ "将类转移到其他文件" }) {
    override fun actionPerformed(e: AnActionEvent) {
        MoveClassToOtherFileActionDialog(e.project!!,e.getDartClassDefinition()!!).show()
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null && e.ele() != null && e.getDartClassDefinition() != null
        super.update(e)
    }


    companion object {

        ///操作实例
        val instance: AnAction get() = ActionManager.getInstance().getAction("MoveClassToOtherFile")
    }

}

///操作弹窗
 class MoveClassToOtherFileActionDialog(project: Project, private val psiElement: DartClassDefinitionImpl) : MyDialogWrapper(project){

     init {
         super.init()
         title = "将类移动到其他文件"
     }
     override fun createCenterPanel(): JComponent {
         return  panel {
             row ("转移到文件") {
                 textFieldWithBrowseButton("选择文件",project, FileChooserDescriptorFactory.createSingleFileDescriptor("dart").withRoots(
                     ProjectRootManager.getInstance(project).contentSourceRoots.first()
                 )).align(Align.FILL)
             }
             row ("转移的类") {
                 label(psiElement.name?:"...?")
             }
             row ("记住此路径") {
                 checkBox("下次自动选择上面的文件路径").bindSelected({true},{})
             }
         }
     }

 }