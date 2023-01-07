package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.DartTokenTypes
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService

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


    private var setting = PluginStateService.getInstance().state ?: AppStateModel()
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {

        val file = parameters.originalFile
        file.findElementAt(parameters.offset)?.apply {
            if (prevSibling.elementType == DartTokenTypes.REGULAR_STRING_PART) {
                val strEle = prevSibling.text
                println(strEle)
                if (strEle.length >= setting.assetCompilationTriggerLen && (strEle.startsWith(setting.assetCompilationTriggerString))) {
                    doHandle(parameters, result)
                }
            }
        }
    }


    private fun doHandle(parameters: CompletionParameters, result: CompletionResultSet) {
        val path = parameters.editor.project?.basePath
        val assetsPath = "$path/${setting.assetScanFolderName}"
        val findFileByUrl = LocalFileSystem.getInstance().findFileByPath(assetsPath)
        findFileByUrl?.apply {
            elementHandle(this, result)
        }.takeIf { findFileByUrl?.isDirectory == true }
    }

    private fun elementHandle(file: VirtualFile, result: CompletionResultSet) {
        try {
            val cs = file.children.toList()
            cs.forEach { f ->
                if (f.isDirectory) {
                    elementHandle(f, result)
                } else {
                    val indexOf = f.path.indexOf("assets")
                    val withIcon =
                        LookupElementBuilder.create(f.path.substring(indexOf)).withIcon(MyIcons.diandianLogoIcon)
                    result.addElement(withIcon)
                }
            }
        } catch (_: InvalidVirtualFileAccessException) {

        }
    }

}
