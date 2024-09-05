package shop.itbug.fluttercheckversionx.util

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.io.FileWriter
import java.io.IOException
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

class MyFileUtil {


    companion object {

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

        fun saveTextToTempFile(project: Project, text: String, fileName: String) {
            try {


                // 获取项目的基路径
                val projectBasePath = project.basePath

                // 定义临时文件夹路径
                val tempDirPath = (projectBasePath + File.separator) + "temp"

                // 创建临时文件夹
                val tempDir: File = File(tempDirPath)
                if (!tempDir.exists()) {
                    tempDir.mkdir()
                }

                // 定义文件路径
                val tempFile = File(tempDir, fileName)

                // 写入文本内容
                val writer = FileWriter(tempFile)
                writer.write(text)
                writer.close()
                VirtualFileManager.getInstance().syncRefresh()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}