package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.dialog.AssetsAutoGenerateClassActionConfigDialog
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil


/// 自动正常资产文件调用
///
class AssetsAutoGenerateClassAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT)
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)!!
        val name = vf.name

        project?.apply {
            var setting = GenerateAssetsClassConfig.getGenerateAssetsSetting()
            var isOk = setting.dontTip
            if (isOk.not()) {
                isOk = AssetsAutoGenerateClassActionConfigDialog(project,callback = {
                    setting = it
                }).showAndGet()
            }
            if (isOk) {
               MyDartPsiElementUtil.autoGenerateAssetsDartClassFile(project,name,false,setting)
            }
        }
    }


    override fun update(e: AnActionEvent) {
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf != null && vf.isDirectory
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}