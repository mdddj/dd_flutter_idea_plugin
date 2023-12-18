package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.impl.DartLibraryStatementImpl
import shop.itbug.fluttercheckversionx.autoCompletion.OnlyProjectFileSearch

object LibTools {


    ///获取库列表
    fun getLibraryFiles(project: Project): MutableCollection<String> {
        val files1 = FileBasedIndex.getInstance().getContainingFiles(
            FileTypeIndex.NAME,
            DartFileType.INSTANCE,
            OnlyProjectFileSearch(project)
        )
        val libraryNames = mutableListOf<String>()
        if (files1.isNotEmpty()) {
            files1.forEach { file ->
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