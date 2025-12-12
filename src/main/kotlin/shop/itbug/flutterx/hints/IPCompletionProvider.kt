package shop.itbug.flutterx.hints

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.ui.UIUtil
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.Util

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
            Util.resolveLocalAddresses().stream()
                .forEach {
                    result.addElement(
                        LookupElementBuilder.create(it.hostAddress).withIcon(MyIcons.flutter)
                            .withItemTextForeground(
                                UIUtil.getLabelInfoForeground()
                            ).withItemTextItalic(true)
                            .withItemTextUnderlined(true)
                    )
                }
        }
    }
}