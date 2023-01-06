package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import shop.itbug.fluttercheckversionx.util.Util

/**
 * DdCheckPlugin().init(MyHttpRequest().getDio(), initHost: '192.168.100.50');
 * initHost 自动补全IP地址功能
 */
class IPCompletionProvider: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
       val offset = parameters.offset
       val file = parameters.originalFile
       val element = file.findElementAt(offset)
        element?.apply {
           val text = element.parent.parent.firstChild.text
            if(text.equals("initHost")){
                Util.resolveLocalAddresses().onEach {
                    result.addElement(LookupElementBuilder.create(it.hostAddress))
                }
            }
        }
    }
}