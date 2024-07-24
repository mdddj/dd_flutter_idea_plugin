package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.impl.DartLibraryStatementImpl

@Service(Service.Level.PROJECT)
class UserDartLibService(val project: Project) : DumbAware {

    private var libraryNames: List<String> = emptyList()


    /// get all user define library
    fun getLibraryNames(): List<String> {
        if (libraryNames.isEmpty()) {
            getLibraryFiles(project)
        }
        return libraryNames
    }

    companion object {
        fun getInstance(project: Project) = project.service<UserDartLibService>()
    }


    private fun getLibraryFiles(project: Project) {
        val lib: VirtualFile =
            LocalFileSystem.getInstance().findFileByPath("${project.basePath}/lib") ?: return
        val files = FileTypeIndex.getFiles(
            DartFileType.INSTANCE,
            GlobalSearchScopes.directoryScope(project, lib, true).uniteWith(LibrarySearchScope(project))
        )
        val libs = mutableListOf<String>()
        if (files.isNotEmpty()) {
            files.forEach { file ->
                val findFile =
                    ReadAction.compute<PsiFile, Exception> { PsiManager.getInstance(project).findFile(file) }
                val libPsiElement = PsiTreeUtil.findChildOfType(findFile, DartLibraryStatementImpl::class.java)
                libPsiElement?.libraryNameElement?.text?.let { libName ->
                    if (libName.isNotEmpty()) {
                        libs.add(libName)
                    }
                }
            }
            libraryNames = libs
        }
    }

}


class LibrarySearchScope(private val myProject: Project) : GlobalSearchScope() {

    override fun contains(file: VirtualFile): Boolean {
        return ApplicationManager.getApplication().runReadAction(Computable { hasLibPsiElement(file) })

    }


    private fun hasLibPsiElement(file: VirtualFile): Boolean {
        val psiFile = PsiManager.getInstance(myProject).findFile(file)
        return PsiTreeUtil.findChildOfType(
            psiFile,
            DartLibraryStatementImpl::class.java
        ) != null
    }


    override fun isSearchInModuleContent(aModule: Module): Boolean {
        return false
    }

    override fun isSearchInLibraries(): Boolean {
        return false
    }

}

class UserDartLibServiceInit : ProjectActivity {
    override suspend fun execute(project: Project) {
        DumbService.getInstance(project).runWhenSmart {
//            getInstance(project).getLibraryNames()
        }
    }

}


