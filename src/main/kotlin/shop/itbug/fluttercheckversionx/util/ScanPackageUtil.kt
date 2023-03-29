package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.DartUriElementBase
import com.jetbrains.lang.dart.util.DartResolveUtil

///扫描未使用的包
object ScanPackageUtil {


    fun doScan(project: Project) {
        val files = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME,DartFileType.INSTANCE,
            GlobalSearchScope.projectScope(project))
        val instance = PsiManager.getInstance(project)

        files.forEach {
            if(it.fileType is DartFileType){
                val findFile = instance.findFile(it)!!
                val impls =  PsiTreeUtil.findChildrenOfAnyType(findFile, DartUriElementBase::class.java)

                impls.forEach { ele ->
                    val  eleText =  ele.text.replace("\"","").replace("'","")
                    val importedFile = DartResolveUtil.getImportedFile(project, it, eleText)
                    if(importedFile!=null){
//                        val partUris = DartAnalysisServerService.getInstance(project).analysis_getHover(it,ele.startOffset)
//                        println("路径:${importedFile.path}")
                        val analysisGethover =
                            DartAnalysisServerService.getInstance(project).analysis_getHover(importedFile, 0)
                        println(analysisGethover)
                    }
                }
            }

        }

    }

}
