package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * 资源文件路径自动补全
 */
class AssetsFilePathAutoComplete : CompletionContributor(){
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val path = parameters.editor.project?.basePath
        val assetsPath = "$path/assets"
        val findFileByUrl = VirtualFileManager.getInstance().findFileByUrl(assetsPath)
        findFileByUrl?.apply {
            elementHandle(this,result)
        }.takeIf { findFileByUrl?.isDirectory == true }

        super.fillCompletionVariants(parameters, result)
    }

    private fun elementHandle(file: VirtualFile, result: CompletionResultSet) {
        file.children.forEach { f ->
            if(f.isDirectory){
                elementHandle(f,result)
            }else{
                result.addElement(MyPathElement(file))
            }
        }
    }
}

class MyPathElement(val file: VirtualFile):LookupElement() {
    override fun getLookupString(): String {
        return file.name
    }

}