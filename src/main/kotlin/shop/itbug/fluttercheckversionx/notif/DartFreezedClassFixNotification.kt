package shop.itbug.fluttercheckversionx.notif

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.PopupHandler
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import shop.itbug.fluttercheckversionx.services.PubspecService
import java.util.function.Function
import javax.swing.JComponent

/**
 * 修复文件中的 freezed 类
 * 需要 用户是否依赖 freezed
 * 需要 freezed 3.0.x
 *
 */
class DartFreezedClassFixNotification : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project, file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (project.isDisposed) return null
        if (PluginConfig.getState(project).showFreezed3FixNotification.not()) return null
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile !is DartFile) return null
        val pubService = PubspecService.getInstance(project)
        val hasUseFreezed = pubService.hasFreezed()
        if (!hasUseFreezed) return null
        if (!hasFreezedClass(psiFile)) return null
        if (!pubService.freezedVersionIsThan3()) return null
        return Function<FileEditor, JComponent?> {

            return@Function Panel(project, psiFile)
        }

    }


    //在文件中是否有 freezed类,true: 有 freezed类
    fun hasFreezedClass(file: DartFile): Boolean {
        val clazzList = PsiTreeUtil.findChildrenOfType(file, DartClassDefinitionImpl::class.java)
        if (clazzList.isEmpty()) return false
        val hasFreezedClass =
            clazzList.map { it.myManagerFun().hasFreezeMetadata() && !it.myManagerFun().isFreezed3Class() }.any { it }
        return hasFreezedClass
    }
}


// 面板
private class Panel(val project: Project, val file: DartFile) : EditorNotificationPanel() {


    private val actionGroup = ActionManager.getInstance().getAction("Fix Freezed Class Tool") as DefaultActionGroup
    val fixFreezedClassTool = createActionLabel("${PluginBundle.get("fix")} freezed 3.0 class") {
        showPopup()
    }

    val closeSetting = createActionLabel(PluginBundle.get("dot_show_again_this")) {
        PluginConfig.changeState(project) {
            it.showFreezed3FixNotification = false
        }
        EditorNotifications.getInstance(project).updateNotifications(file.virtualFile)
    }

    private fun showPopup() {
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                "${PluginBundle.get("fix")} freezed class",
                actionGroup,
                DataManager.getInstance().getDataContext(this),
                JBPopupFactory.ActionSelectionAid.MNEMONICS,             // 是否允许过滤
                true,              // 是否显示图标
                {},             //
                2
            ).showUnderneathOf(fixFreezedClassTool)
    }


    init {
        icon(MyIcons.dartPluginIcon)
        text(PluginBundle.get("w.t"))


        myLinksPanel.add(closeSetting)
        myLinksPanel.add(fixFreezedClassTool)

        createActionListPop()

    }


    //创建菜单
    private fun createActionListPop() {
        PopupHandler.installPopupMenu(fixFreezedClassTool, "Fix Freezed Class Tool", "Fix Freezed Class Tool")
    }
}




