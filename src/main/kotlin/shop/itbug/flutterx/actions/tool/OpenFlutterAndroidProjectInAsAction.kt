package shop.itbug.flutterx.actions.tool

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.util.SystemInfo
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.tools.FlutterProjectUtil


/// 打开Android项目在Android studio
class OpenFlutterAndroidProjectInAsAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project ?: return
        val runTool = FlutterProjectUtil(project)
        runTool.openAndroidStudioWithDirectory()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = MyIcons.flutter
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            e.project != null && file != null && file.exists() && file.isDirectory && file.name == "android"
        super.update(e)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}


/// 在xcode打开ios目录
class OpenFlutterIosProjectInAsAction : AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val project = p0.project ?: return
        val runTool = FlutterProjectUtil(project)
        runTool.openIosInXCode()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.icon = MyIcons.flutter
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible =
            e.project != null && file != null && file.exists() && file.isDirectory && file.name == "ios" && SystemInfo.isMac
        super.update(e)
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

