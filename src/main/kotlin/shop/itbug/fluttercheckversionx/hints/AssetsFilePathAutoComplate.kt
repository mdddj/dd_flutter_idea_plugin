package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import shop.itbug.fluttercheckversionx.icons.MyIcons

/**
 * 资源文件路径自动补全
 */
class AssetsFilePathAutoComplete : CompletionContributor(){
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val path = parameters.editor.project?.basePath
        val assetsPath = "$path/assets"
        val findFileByUrl = LocalFileSystem.getInstance().findFileByPath(assetsPath)
        findFileByUrl?.apply {
            elementHandle(this,result)
        }.takeIf { findFileByUrl?.isDirectory == true }
        super.fillCompletionVariants(parameters, result)
    }

    private fun elementHandle(file: VirtualFile, result: CompletionResultSet) {
        val cs = file.children.toList()
        cs.forEach { f ->
            if(f.isDirectory){
                elementHandle(f,result)
            }else{
                val indexOf = f.path.indexOf("assets")
                val withIcon = LookupElementBuilder.create(f.path.substring(indexOf)).withIcon(MyIcons.imageIcon)
                result.addElement(withIcon)
            }
        }
    }
}
