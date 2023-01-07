package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile


typealias HandleVirtualFile = (virtualFile: VirtualFile) -> Unit
fun VirtualFile.fileNameWith(folderName: String) : String {
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
    }
}