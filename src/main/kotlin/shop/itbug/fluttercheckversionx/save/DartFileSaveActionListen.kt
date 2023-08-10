package shop.itbug.fluttercheckversionx.save

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.toastWithError
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader


class DartFileDocumentSynchronizationVetoer : FileDocumentSynchronizationVetoer() {


    var indicator : ProgressIndicator? = null

    override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {

        val state = DartFileSaveSettingState.getInstance().state
        if (isSaveExplicit && state.enable) {
            runCommand(state, document)
        }
        return super.maySaveDocument(document, isSaveExplicit)
    }

    private fun runCommand(state: DartFileSaveSettingModel, document: Document) {
        var command = state.command

        if (command.contains("{path}")) {
            command = command.replace("{path}", document.getPath())
        }
        document.getProject()?.let {
            if (state.runType) {
                if (command.isNotBlank()) {
                    RunUtil.runCommand(it, state.title.ifBlank { "Dog" }, command)
                    println("执行完毕")
                }

            } else {
                if (command.isNotBlank()) {
                    if(indicator!=null && indicator!!.isRunning){
                        indicator!!.cancel()
                    }
                    val  task = createTask(command, it,state)
                    ProgressManager.getInstance().run(task)
                }
            }
        }
    }

    private fun createTask(command: String, project: Project,state: DartFileSaveSettingModel): Task.Backgroundable {
        return object : Task.Backgroundable(project, state.title.ifBlank { command }) {
            override fun run(indicator: ProgressIndicator) {
                this@DartFileDocumentSynchronizationVetoer.indicator = indicator
                try {
                    val exec = Runtime.getRuntime().exec(command,null, File(project.basePath?:""))
                    val code = exec.waitFor()
                    if(code != 0){
                        val errorStream: InputStream = exec.errorStream
                        val errorReader = BufferedReader(InputStreamReader(errorStream))
                        var errorLine: String
                        while (errorReader.readLine().also { errorLine = it } != null) {
                            project.toastWithError("任务非正常结束:$errorLine")
                        }
                    }
                    exec.destroy()
                }catch (e:Exception){
                    project.toastWithError("执行任务失败:$command  $e ")
                }
                this@DartFileDocumentSynchronizationVetoer.indicator = null
            }

        }
    }

//     fun Document.reformat() {
//         getProject()?.let {
//             getFile()?.apply {
//                     PsiManager.getInstance(it).findFile(this)?.reformatText()
//             }
//         }
//     }

    private fun getProjectFun(document: Document): Project? {
        val file = FileDocumentManager.getInstance().getFile(document)
        val openProjects = ProjectManager.getInstance().openProjects
        if (file != null) {
            openProjects.forEach {
                if (file.path.startsWith(it.basePath ?: "")) {
                    return it
                }
            }
        }
        return null
    }

    private fun Document.getFile(): VirtualFile? {
        return FileDocumentManager.getInstance().getFile(this)
    }
    private fun Document.getPath(): String {
        return getFile()?.path ?: ""
    }

    private fun Document.getProject(): Project? = getProjectFun(this)
}


