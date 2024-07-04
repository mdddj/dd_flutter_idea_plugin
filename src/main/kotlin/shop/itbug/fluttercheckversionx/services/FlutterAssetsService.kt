package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

private typealias HandleVirtualFile = (virtualFile: VirtualFile) -> Unit


// assets
@Service(Service.Level.PROJECT)
class FlutterAssetsService(val project: Project) {


    companion object {
        fun getInstance(project: Project): FlutterAssetsService = project.getService(FlutterAssetsService::class.java)
    }

    private var assets: MutableList<String> = mutableListOf()

    fun allAssets(): MutableList<String> = assets

    fun init(folderName: String) {
        assets.clear()
        onFolderEachWithProject(folderName) {
            assets.add(it.fileNameWith(folderName))
        }
    }


    private fun onFolderEachWithProject(folderName: String, handle: HandleVirtualFile) {
        val path = project.basePath + "/$folderName"
        val findFileByPath = LocalFileSystem.getInstance().findFileByPath(path)
        findFileByPath?.apply {
            virtualFileHandle(this, handle)
        }
    }

    private fun virtualFileHandle(file: VirtualFile, handle: HandleVirtualFile) {
        if (file.isDirectory) {
            try {
                val cs = file.children.toList()
                cs.forEach { f ->
                    if (f.isDirectory) {
                        virtualFileHandle(f, handle)
                    } else {
                        handle.invoke(f)
                    }
                }
            } catch (_: InvalidVirtualFileAccessException) {
            }
        }
    }

    fun VirtualFile.fileNameWith(folderName: String): String {
        val indexOf = this.path.indexOf(folderName)
        return this.path.substring(indexOf)
    }

}