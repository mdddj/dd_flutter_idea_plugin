package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import shop.itbug.fluttercheckversionx.dialog.AssetsAutoGenerateClassActionConfigDialog
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.Util
import shop.itbug.fluttercheckversionx.util.fileNameWith


/// 自动正常资产文件调用
///
class AssetsAutoGenerateClassAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val names = mutableSetOf<String>()

        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)!!
        val name = vf.name

        project?.apply {

            val isOk = AssetsAutoGenerateClassActionConfigDialog(project).showAndGet()
            if (!isOk) {
                println("取消生成")
                return
            }

            val classElement =
                MyDartPsiElementUtil.createDartClassBodyFromClassName(project, "AppAssets")
            classElement.classBody?.classMembers?.let { classMembers ->
                MyFileUtil.onFolderEachWithProject(project, name) { virtualFile ->
                    val eleValue = virtualFile.fileNameWith(name)
                    var filename = Util.removeSpecialCharacters(virtualFile.presentableName.split(".").first())
                    if (names.contains(filename)) filename += "${names.filter { it.contains(filename) }.size}"
                    if (filename.isNotEmpty() && eleValue.isNotEmpty()) {
                        val expression = "static const $filename = '$eleValue'"
                        names.add(filename)
                        MyDartPsiElementUtil.createVarExpressionFromText(
                            project,
                            expression
                        ).let {
                            val d = MyDartPsiElementUtil.createLeafPsiElement(project)
                            runWriteAction {
                                it?.addAfter(d, it.nextSibling)
                                it?.let { it1 -> classMembers.addAfter(it1, classMembers.nextSibling) }
                            }
                        }

                    }
                }
            }

            val file = MyDartPsiElementUtil.createDartFileWithElement(project, classElement, "lib", "R.dart", null)

            file?.let {
                WriteCommandAction.runWriteCommandAction(project) {
                    CodeStyleManager.getInstance(project).reformat(file)
                }
            }
        }
    }


    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf != null && vf.isDirectory
        super.update(e)
    }
}