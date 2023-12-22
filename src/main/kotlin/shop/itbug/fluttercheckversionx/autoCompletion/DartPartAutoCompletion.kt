package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.DartLibraryStatementImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * part lib自动完成提供者
 */
class DartPartAutoCompletion : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE),
               object : CompletionProvider<CompletionParameters>(){
                   override fun addCompletions(
                       p0: CompletionParameters,
                       p1: ProcessingContext,
                       p2: CompletionResultSet
                   ) {
                       val project = p0.editor.project!!
                       val files = FileBasedIndex.getInstance().getContainingFiles(
                           FileTypeIndex.NAME,
                           DartFileType.INSTANCE,
                           OnlyProjectFileSearch(project)
                       )
                       files.forEach { file -> 
                           val findFile = PsiManager.getInstance(project).findFile(file)!!
                           val findChildrenOfAnyType =
                               PsiTreeUtil.findChildrenOfAnyType(findFile, DartLibraryStatementImpl::class.java)
                           findChildrenOfAnyType.forEach { lib ->
                               val ele = LookupElementBuilder.create("part of ${lib.libraryNameElement?.text?:""}").withIcon(
                                   MyIcons.flutter).withPsiElement(MyDartPsiElementUtil.createDartPart("part of ${lib.libraryNameElement?.text}",project))
                               p2.addElement(ele)
                           }
                       }
                      
                   }

               }
            )
    }
}


class OnlyProjectFileSearch(private val myProject: Project): GlobalSearchScope(myProject) {
    
    private val fileIndexs =   ProjectRootManager.getInstance(myProject).fileIndex
    
    override fun contains(p0: VirtualFile): Boolean {
        val psiFile = PsiManager.getInstance(myProject).findFile(p0)
        return fileIndexs.isInSourceContent(p0) && PsiTreeUtil.findChildrenOfAnyType(psiFile,DartLibraryStatementImpl::class.java).isNotEmpty()
    }

    override fun isSearchInModuleContent(p0: Module): Boolean {
       return false
    }

    override fun isSearchInLibraries(): Boolean {
       return false
    }

}