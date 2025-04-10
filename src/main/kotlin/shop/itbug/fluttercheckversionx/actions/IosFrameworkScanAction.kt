package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.JComponent


class IosFrameworkScanActionDialog(val project: Project, val e: AnActionEvent) : DialogWrapper(project) {

    data class ModelProperties(var scanDirectionPath: String, var extension: String, var isDirection: Boolean)

    val model = ModelProperties(scanDirectionPath = "/Framework", extension = "framework", isDirection = false)

    private lateinit var dialogPanel: DialogPanel

    init {
        super.init()
        title = "Flutter IOS Framework Util"
    }

    override fun createCenterPanel(): JComponent {
        dialogPanel = panel {
            row("扫描目录") {
                textField().bindText(model::scanDirectionPath)
            }
            row("模块后缀") {
                textField().bindText(model::extension)
            }

            row {
                checkBox(PluginBundle.get("is_dir")).bindSelected({ model.isDirection }, { model.isDirection = it })
            }

        }
        return dialogPanel
    }


    override fun doOKAction() {
        dialogPanel.apply()
        startTask()
        super.doOKAction()
    }

    private fun startTask() {
        val task = object : Task.Backgroundable(project, PluginBundle.get("scaning") + " - ios Framework") {
            override fun run(indicator: ProgressIndicator) {
                startTask(project, e)
            }
        }
        ProgressManager.getInstance().run(task)
    }

    //开始任务
    fun startTask(project: Project, e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (file != null) {
            val frameworkDirection = LocalFileSystem.getInstance().findFileByPath(file.path + model.scanDirectionPath)
            if (frameworkDirection != null && frameworkDirection.isDirectory) {
                val sb = StringBuffer()
                VfsUtilCore.visitChildrenRecursively(frameworkDirection, object : VirtualFileVisitor<Void>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        if (file.extension == model.extension) {
                            val path = file.path.replace(project.basePath + "/ios/", "")
                            sb.append("\"$path\",")
                        }
                        if (model.isDirection && file.name.endsWith(model.extension)) {
                            val path = file.path.replace(project.basePath + "/ios/", "")
                            sb.append("\"$path\",")
                        }
                        return super.visitFile(file)
                    }

                })

                val finalString = sb.toString().removeSuffix(",")
                finalString.copyTextToClipboard()
                project.toast(finalString)
            } else {
                project.toastWithError("Framework directory not found")
            }
        }
    }

}


///扫描ios插件包的Framework
class IosFrameworkScanAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            IosFrameworkScanActionDialog(it, e).show()
        }
    }


    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}