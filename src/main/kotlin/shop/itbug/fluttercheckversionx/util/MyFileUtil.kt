package shop.itbug.fluttercheckversionx.util

import com.intellij.json.JsonFileType
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import com.jetbrains.lang.dart.DartFileType
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.util.filetool.VisitFolderCheckResult
import java.io.File
import java.nio.file.Path


typealias HandleVirtualFile = (virtualFile: VirtualFile) -> Unit

fun VirtualFile.fileNameWith(folderName: String): String {
    val indexOf = this.path.indexOf(folderName)
    return this.path.substring(indexOf)
}

object MyFileUtil {


    /**
     * 遍历文件夹目录内所有的文件
     * @param folderName 项目文件夹目录
     * @param handle 处理遍历到的文件
     */
    fun onFolderEachWithProject(project: Project, folderName: String, handle: HandleVirtualFile) {
        val projectPath = project.guessProjectDir()
        if (projectPath != null) {
            val folder = projectPath.findFileByRelativePath(folderName)
            if (folder != null) {
                virtualFileHandle(folder, handle)
            }
        }
    }


    /**
     * 获取virtualFile 的 root 目录
     */
    fun getRootVirtualFile(virtualFile: VirtualFile, project: Project): VirtualFile? {
        return ProjectFileIndex.getInstance(project).getContentRootForFile(virtualFile)
            ?: WorkspaceFileIndex.getInstance(project).getContentFileSetRoot(virtualFile, false)
    }

    /**
     *
     */
    private fun virtualFileHandle(file: VirtualFile, handle: HandleVirtualFile) {
        VfsUtilCore.visitChildrenRecursively(file, VisitFolderCheckResult(handle))
    }

    fun fileIsExists(path: String): Boolean {
        val vf = VirtualFileManager.getInstance().findFileByNioPath(Path.of(path)) ?: return false
        return LocalFileSystem.getInstance().exists(vf)
    }

    fun pathIsExists(path: Path): Boolean {
        val vf = VirtualFileManager.getInstance().findFileByNioPath(path) ?: return false
        return LocalFileSystem.getInstance().exists(vf)
    }


    /**
     * 获取flutter项目的pubspec.yaml 文件
     */
    fun getPubspecVirtualFile(project: Project): VirtualFile? {
        val dir = project.guessProjectDir() ?: return null
        return dir.findFileByRelativePath("pubspec.yaml")
    }

    /**
     * 获取flutter项目的pubspec.yaml psi文件
     */
    fun getPubspecFile(project: Project): YAMLFile? {
        val file = getPubspecVirtualFile(project) ?: return null
        return PsiManager.getInstance(project).findFile(file) as? YAMLFile
    }


    /**
     * 重新索引pubspec文件
     */
    fun reIndexPubspecFile(project: Project) {
        getPubspecVirtualFile(project)?.let { virtualFile ->
            if (checkFileIsIndex(project, virtualFile)) {
                FileBasedIndex.getInstance().requestReindex(virtualFile)
            }
        }
    }

    fun reIndexWithVirtualFile(virtualFile: VirtualFile) {
        FileBasedIndex.getInstance().requestReindex(virtualFile)
    }


    /**
     * 检查文件是否被索引?
     */
    fun checkFileIsIndex(project: Project, file: VirtualFile): Boolean {
        return try {
            val file = runReadAction { PsiManager.getInstance(project).findFile(file) }
            file != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 检查文件是否被索引,协程版本
     */
    suspend fun checkFileIsIndexByXc(project: Project, file: VirtualFile): Boolean {
        return try {
            val file = readAction { PsiManager.getInstance(project).findFile(file) }
            file != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 重新索引指定文件
     */
    fun reIndexFile(project: Project, file: VirtualFile) {
        if (checkFileIsIndex(project, file)) {
            FileBasedIndex.getInstance().requestReindex(file)
        }
    }

    /**
     * 重新索引指定文件,协程版本
     */
    suspend fun reIndexFileByXc(project: Project, file: VirtualFile) {
        if (checkFileIsIndexByXc(project, file)) {
            FileBasedIndex.getInstance().requestReindex(file)
        }
    }

    /**
     * 获取项目所有的dart文件
     */
    fun findAllProjectFiles(project: Project): List<VirtualFile> {
        val lib: VirtualFile =
            LocalFileSystem.getInstance().findFileByPath("${project.basePath}" + File.separator + "lib")
                ?: return emptyList()
        val scope = GlobalSearchScopes.directoryScope(project, lib, true)
        val files = FileTypeIndex.getFiles(DartFileType.INSTANCE, scope)
        return files.toList()
    }

    /**
     * 创建一个json的虚拟文件
     */
    fun createVirtualFileByJsonText(
        text: String,
        filename: String,
        action: (file: VirtualFile, tool: MyFileUtil) -> Unit
    ): VirtualFile {
        val vf = LightVirtualFile(filename, JsonFileType.INSTANCE, text)
        action.invoke(vf, this)
        return vf
    }

    /**
     * 打开某个文件
     */
    fun openInEditor(file: VirtualFile, project: Project) {
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    /**
     * 格式化文件
     */
    fun reformatVirtualFile(file: VirtualFile, project: Project) {
        val vf = runReadAction { PsiManager.getInstance(project).findFile(file) }
        if (vf != null) {
            val task = object : Task.Backgroundable(project, "Reformat ${file.name}", false) {
                override fun run(p0: ProgressIndicator) {
                    WriteCommandAction.runWriteCommandAction(project) {
                        CodeStyleManager.getInstance(project).reformat(vf)
                    }
                }
            }
            task.queue()

        }
    }


    //检查某个资产文件是否存在
    fun hasFileInAssetPath(assets: String, project: Project): Boolean {
        val projectPath = project.guessProjectDir()?.path ?: return false
        val fullPath = projectPath + File.separator + assets
        return File(fullPath).exists()
    }

}