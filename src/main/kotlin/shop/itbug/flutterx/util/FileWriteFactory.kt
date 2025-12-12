package shop.itbug.flutterx.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.lang.dart.DartFileType
import shop.itbug.flutterx.i18n.PluginBundle
import kotlin.io.path.Path

typealias WriteFileSuccess = (PsiElement, VirtualFile) -> Unit

@Service(Service.Level.PROJECT)
class FileWriteService(val project: Project) {

    /**
     * 写入到项目目录中,注意写入前判断一下路径
     */
    fun writeTo(text: String, fileName: String, projectDirectory: String, onSuccess: WriteFileSuccess? = null) {
        val dirVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(Path(projectDirectory))
        if (dirVirtualFile != null) {
            val findDirectory = PsiManager.getInstance(project).findDirectory(dirVirtualFile)
            if (findDirectory != null) {
                val createDartFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("$fileName.dart", DartFileType.INSTANCE, text)
                try {
                    val psiElement = ApplicationManager.getApplication()
                        .runWriteAction(Computable { findDirectory.add(createDartFile) })
                    onSuccess?.invoke(psiElement, psiElement.containingFile.virtualFile)
                } catch (e: Exception) {
                    showErrorMessage("${PluginBundle.get("freezed.gen.create.error")}:${e.localizedMessage}")
                }
            }
        }
    }

    private fun showErrorMessage(msg: String) {
        val groupId = "json_to_freezed_tooltip"
        NotificationGroupManager.getInstance().getNotificationGroup(groupId)
            .createNotification(msg, NotificationType.ERROR).notify(project)
    }


    companion object {
        fun getInstance(project: Project): FileWriteService = project.getService(FileWriteService::class.java)
    }
}