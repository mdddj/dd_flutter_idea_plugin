package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import shop.itbug.fluttercheckversionx.icons.MyIcons

/**
 * 资源文件路径自动补全
 */
class AssetsFilePathAutoComplete : CompletionContributor() {



    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE),
            AssetsFilePathAutoCompleteProvider()
        )
    }


}


/**
 * 资产文件自动补全
 */
class AssetsFilePathAutoCompleteProvider : CompletionProvider<CompletionParameters>() {

    val defaultTag = "ass" //触发关键字

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val file = parameters.originalFile
        file.findElementAt(parameters.offset)?.apply {
            if (parent is DartStringLiteralExpression) {
                val strEle = parent as DartStringLiteralExpression
                if(strEle.textLength>=3 || strEle.text.contains(defaultTag)) {
                    doHandle(parameters,result)
                }
            }
        }
    }


    fun doHandle(parameters: CompletionParameters, result: CompletionResultSet) {
        val path = parameters.editor.project?.basePath
        val assetsPath = "$path/assets"
        val findFileByUrl = LocalFileSystem.getInstance().findFileByPath(assetsPath)
        findFileByUrl?.apply {
            elementHandle(this, result)
        }.takeIf { findFileByUrl?.isDirectory == true }
    }

    private fun elementHandle(file: VirtualFile, result: CompletionResultSet) {
        val cs = file.children.toList()
        cs.forEach { f ->
            if (f.isDirectory) {
                elementHandle(f, result)
            } else {
                val indexOf = f.path.indexOf("assets")
                val withIcon = LookupElementBuilder.create(f.path.substring(indexOf)).withIcon(MyIcons.diandianLogoIcon)
                result.addElement(withIcon)
            }
        }
    }

}
