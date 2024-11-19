package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.impl.DartLibraryStatementImpl
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.util.MyFileUtil

data class DartPartLibModel(var file: VirtualFile, val libName: String)
data class DartPartPath(var libName: String, var path: String, val file: VirtualFile)
private data class FileAndElement(val file: VirtualFile, val element: DartLibraryStatementImpl)

@Service(Service.Level.PROJECT)
class UserDartLibService(val project: Project) : DumbAware {

    private var libraryNames: MutableSet<String> = hashSetOf()
    private var virtualFiles: MutableSet<DartPartLibModel> = hashSetOf() //定义了libaray的文件列表


    /// get all user define library
    fun getLibraryNames(): MutableSet<String> {
        return libraryNames
    }

    /**
     * 计算[file]的相对地址
     */
    fun calcRelativelyPath(file: VirtualFile): HashSet<DartPartPath> {
        val strs = virtualFiles.map {
            try {
                val path = file.parent.toNioPath().relativize(it.file.toNioPath()).normalize().toString()
                return@map DartPartPath(it.libName, path, it.file)
            } catch (_: Exception) {
                return@map null
            }
        }.filterNotNull().toHashSet()
        return strs.sortedBy { it.path.length }.toHashSet()
    }


    companion object {
        fun getInstance(project: Project) = project.service<UserDartLibService>()
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun collectPartOf() {
        libraryNames = hashSetOf()
        virtualFiles = hashSetOf()
        GlobalScope.launch {
            val files = readAction { MyFileUtil.findAllProjectFiles(project) }
            val result: List<FileAndElement> = files.map {
                async {
                    readAction {
                        val file = PsiManager.getInstance(project).findFile(it) ?: return@readAction null
                        val element = PsiTreeUtil.findChildOfType(
                            file, DartLibraryStatementImpl::class.java
                        ) ?: return@readAction null
                        FileAndElement(file.virtualFile, element)
                    }
                }
            }.awaitAll().toList().filterNotNull()
            result.forEach { (file, element) ->
                runReadAction {
                    val libName = element.libraryNameElement?.text ?: ""
                    libraryNames.add(libName)
                    virtualFiles.add(DartPartLibModel(file, libName))
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


