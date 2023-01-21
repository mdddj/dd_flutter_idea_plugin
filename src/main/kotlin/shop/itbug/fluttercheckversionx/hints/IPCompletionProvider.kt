package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.Util

/**
 * DdCheckPlugin().init(MyHttpRequest().getDio(), initHost: '192.168.100.50');
 * initHost 自动补全IP地址功能
 */
class IPCompletionProvider : CompletionProvider<CompletionParameters>() {
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
            if (text.equals("initHost")) {
                Util.resolveLocalAddresses().
                    stream()
                    .filter { it.hostAddress!= "127.0.0.1" }
                    .forEach {
                    result.addElement(
                        LookupElementBuilder.create(it.hostAddress).withIcon(MyIcons.diandianLogoIcon)
                            .withItemTextForeground(
                                UIUtil.getLabelInfoForeground()
                            ).withItemTextItalic(true)
                            .withItemTextUnderlined(true)
                            .appendTailText("  梁典典: dart连接IDEA Diox监听的IP地址",true)
                    )
                }
            }
        }
    }
}