package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.services.PubService

/// 自动加载版本号
class PluginVersionHints : CompletionContributor() {

    //    填充完成事件
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val currLinePluginName = parameters.originalPosition?.parent?.parent?.firstChild?.text
        if (currLinePluginName != null) {
            runBlocking {
                launch {
                    try {
                        val data = PubService.getPackageVersions(currLinePluginName)
                        data?.versions?.forEach {
                            result.addElement(LookupElementBuilder.create(it))
                        }
                    } catch (_: Exception) {
                    }

                }
            }
        }

        super.fillCompletionVariants(parameters, result)
    }

}