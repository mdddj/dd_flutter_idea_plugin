import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.impl.DartLibraryStatementImpl

@Service(Service.Level.PROJECT)
class UserDartLibService(private val project: Project) : DumbAware {

    private var libraryNames: MutableList<String>? = null

    init {
        DumbService.getInstance(project).runReadActionInSmartMode {
            getLibraryNames()
        }
    }

    /// get all user define library
    fun getLibraryNames(): MutableList<String> {
        if (libraryNames.isNullOrEmpty()) {
            libraryNames = getLibraryFiles(project)
        }
        return libraryNames!!
    }

    companion object {
        fun getInstance(project: Project) = project.service<UserDartLibService>()
    }


    private fun getLibraryFiles(project: Project): MutableList<String> {
        val lib: VirtualFile = LocalFileSystem.getInstance().findFileByPath("${project.basePath}/lib")
            ?: return mutableListOf()
        val files = FileTypeIndex.getFiles(DartFileType.INSTANCE, GlobalSearchScopes.directoryScope(project, lib, true))
        val libraryNames = mutableListOf<String>()
        if (files.isNotEmpty()) {
            files.forEach { file ->
                val findFile = PsiManager.getInstance(project).findFile(file)
                val findChildrenOfAnyType =
                    PsiTreeUtil.findChildrenOfAnyType(findFile, DartLibraryStatementImpl::class.java)
                findChildrenOfAnyType.forEach { libPsi ->
                    libPsi.libraryNameElement?.text?.let { libName ->
                        if (libName.isNotEmpty()) {
                            libraryNames.add(libName)
                        }
                    }
                }
            }
        }
        return libraryNames
    }
}


class OnlyProjectFileSearch(private val myProject: Project) : GlobalSearchScope(myProject) {

    private val fileIndex = ProjectRootManager.getInstance(myProject).fileIndex

    override fun contains(p0: VirtualFile): Boolean {
        val psiFile = runReadAction { PsiManager.getInstance(myProject).findFile(p0) }
        println("is in source ${fileIndex.isInSourceContent(p0)}")
        return fileIndex.isInSourceContent(p0) && PsiTreeUtil.findChildOfAnyType(
            psiFile, DartLibraryStatementImpl::class.java
        ) != null
    }

    override fun isSearchInModuleContent(p0: Module): Boolean {
        return false
    }

    override fun isSearchInLibraries(): Boolean {
        return false
    }

}