package shop.itbug.fluttercheckversionx.notif

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.intellij.ui.PopupHandler
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.actions.freezed.Freezed3ClassFixAction
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.manager.myManagerFun
import shop.itbug.fluttercheckversionx.services.PubspecService
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.MyFileUtil
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


    private var useSelectKeyword = ""
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

    val fixAllInProject = createActionLabel("${PluginBundle.get("fix")} freezed 3.0 class (all)") {
        fixAll()
    }


    val task = object : Task.Modal(project, "", true) {
        override fun run(p0: ProgressIndicator) {
            p0.text = "${PluginBundle.get("scan_privacy")}..."
            val sourcesFiles = runReadAction { MyFileUtil.findAllProjectFiles(project) }
            val freezedClasses = runBlocking {
                sourcesFiles.map { file ->
                    p0.text2 = file.name
                    async {
                        readAction {
                            DartPsiElementUtil.findAllFreezedClassNot3Version(
                                file,
                                project
                            )
                        }
                    }
                }
                    .awaitAll()
            }.flatten()
            println(freezedClasses.size)
            if (freezedClasses.isNotEmpty()) {
                p0.text = "Fixing"
                runBlocking {
                    freezedClasses.map { element ->
                        async {
                            p0.text2 = "fix ${readAction { element.myManagerFun().className }}"
                            Freezed3ClassFixAction.fix(
                                element,
                                Freezed3ClassFixAction.createElementByXc(getIElementType(), project), project
                            )
                        }
                    }.awaitAll()
                }
            }
        }
    }

    private fun getIElementType(): IElementType {
        if (useSelectKeyword == "sealed") {
            return DartTokenTypes.SEALED
        }
        return DartTokenTypes.ABSTRACT
    }

    //修复项目中全部的 freezed class
    private fun fixAll() {
        JBPopupFactory.getInstance().createPopupChooserBuilder<String>(listOf("sealed", "abstract"))
            .setItemChosenCallback {
                println("选择了:$it")
                askOk(it)
            }
            .createPopup()
            .showUnderneathOf(fixAllInProject)
    }

    private fun askOk(string: String) {
        val result = Messages.showYesNoDialog(
            project,
            PluginBundle.get("freezed3_fixall_in_project_content") + "(${string})",
            "${PluginBundle.get("are_you_ok_betch_insert_privacy_file_title")} Action",
            Messages.getQuestionIcon()
        )
        if (result == Messages.YES) {
            doScanTask(string)
        }
    }

    private fun doScanTask(string: String) {
        useSelectKeyword = string
        task.queue()
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
        myLinksPanel.add(fixAllInProject)
        myLinksPanel.add(fixFreezedClassTool)

        createActionListPop()

    }


    //创建菜单
    private fun createActionListPop() {
        PopupHandler.installPopupMenu(fixFreezedClassTool, "Fix Freezed Class Tool", "Fix Freezed Class Tool")
    }
}




