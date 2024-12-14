package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import java.io.File

data class AssetsModel(
    val text: String,
    val file: VirtualFile,
)

// assets
@Service(Service.Level.PROJECT)
class FlutterAssetsService(val project: Project) : VirtualFileVisitor<VirtualFile>(NO_FOLLOW_SYMLINKS), Disposable {
    private val setting: AppStateModel = PluginStateService.getInstance().state ?: AppStateModel()
    private val folderName = setting.assetScanFolderName

    companion object {
        fun getInstance(project: Project): FlutterAssetsService = project.getService(FlutterAssetsService::class.java)
    }

    private var assets: MutableList<AssetsModel> = mutableListOf()

    fun allAssets(): MutableList<AssetsModel> = assets

    fun startInit() {
        assets.clear()
        onFolderEachWithProject(folderName)
    }


    override fun visitFileEx(file: VirtualFile): Result {
        assets.add(AssetsModel(file.fileNameWith(folderName), file))
        return super.visitFileEx(file)
    }


    private fun onFolderEachWithProject(folderName: String) {
        val path = project.basePath + "${File.separator}$folderName"
        val findFileByPath = LocalFileSystem.getInstance().findFileByPath(path) ?: return
        VfsUtilCore.visitChildrenRecursively(findFileByPath, this)
    }


    fun VirtualFile.fileNameWith(folderName: String): String {
        val indexOf = this.path.indexOf(folderName)
        return this.path.substring(indexOf)
    }

    override fun dispose() {
    }

}