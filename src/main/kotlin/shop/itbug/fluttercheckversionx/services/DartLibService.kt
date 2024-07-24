package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.impl.DartLibraryStatementImpl
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

@Service(Service.Level.PROJECT)
class UserDartLibService(val project: Project) : DumbAware {

    private var libraryNames: MutableList<String> = mutableListOf()


    /// get all user define library
    fun getLibraryNames(): List<String> {
        return libraryNames
    }

    companion object {
        fun getInstance(project: Project) = project.service<UserDartLibService>()
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun collectPartOf() {

        GlobalScope.launch {
            readAction {
                val lib: VirtualFile =
                    LocalFileSystem.getInstance().findFileByPath("${project.basePath}" + File.separator + "lib")
                        ?: return@readAction
                val scope = GlobalSearchScopes.directoryScope(project, lib, true)
                val files = FileTypeIndex.getFiles(DartFileType.INSTANCE, scope)
                if (files.isNotEmpty()) {
                    files.forEach { file ->
                        val findFile =
                            ReadAction.compute<PsiFile, Exception> { PsiManager.getInstance(project).findFile(file) }
                        val libPsiElement = PsiTreeUtil.findChildOfType(findFile, DartLibraryStatementImpl::class.java)
                        libPsiElement?.libraryNameElement?.text?.let { libName ->
                            if (libName.isNotEmpty()) {
                                libraryNames.add(libName)
                            }
                        }
                    }
                }
            }
        }
    }

}

class UserDartLibServiceInit : ProjectActivity {
    override suspend fun execute(project: Project) {
        DumbService.getInstance(project).runWhenSmart {
            UserDartLibService.getInstance(project).collectPartOf()
        }
    }
}


