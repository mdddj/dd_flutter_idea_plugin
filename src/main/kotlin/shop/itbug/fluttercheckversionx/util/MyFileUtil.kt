package shop.itbug.fluttercheckversionx.util

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.lang.dart.DartFileType
import org.jetbrains.yaml.psi.YAMLFile
import java.io.File
import java.nio.file.Path
import javax.swing.SwingUtilities


///重新启用分析
fun Project.restartAnalyzer() {
    SwingUtilities.invokeLater {
        runReadAction {
            DaemonCodeAnalyzer.getInstance(this).restart()
        }
    }
}

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
        val path = project.basePath + "/$folderName"
        val findFileByPath = LocalFileSystem.getInstance().findFileByPath(path)
        findFileByPath?.apply {
            virtualFileHandle(this, handle)
        }
    }

    /**
     *
     */
    private fun virtualFileHandle(file: VirtualFile, handle: HandleVirtualFile) {
        if (file.isDirectory) {
            val cs = file.children.toList()
            cs.forEach { f ->
                if (f.isDirectory) {
                    virtualFileHandle(f, handle)
                } else {
                    handle.invoke(f)
                }
            }
        }
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
        val filePath = dir.path + File.separator + "pubspec.yaml"
        val file = VirtualFileManager.getInstance().findFileByNioPath(Path.of(filePath)) ?: return null
        return file
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
            StartupManager.getInstance(project).runAfterOpened {
                DumbService.getInstance(project).runWhenSmart {
                    println("reindex pubspec.yaml")
                    FileBasedIndex.getInstance().requestReindex(virtualFile)
                }
            }
        }
    }

    /**
     * 重新索引指定文件
     */
    fun reIndexFile(project: Project, file: VirtualFile) {
        StartupManager.getInstance(project).runAfterOpened {
            DumbService.getInstance(project).runWhenSmart {
                FileBasedIndex.getInstance().requestReindex(file)
            }
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
}