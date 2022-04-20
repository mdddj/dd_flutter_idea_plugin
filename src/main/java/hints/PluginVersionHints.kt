package hints

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import services.PubService
import services.ServiceCreate
import services.await

/// 自动加载版本号
class PluginVersionHints : CompletionContributor() {

    //    填充完成事件
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val currLinePluginName = parameters.originalPosition?.parent?.parent?.firstChild?.text
        if (currLinePluginName != null) {
            runBlocking {
                launch {
                   try {
                       val data =
                           ServiceCreate.create(PubService::class.java).getPackageVersions(currLinePluginName).await()
                       data.versions.forEach {
                           result.addElement(LookupElementBuilder.create(it))
                       }
                   }catch (e: Exception){
                       println("无法加载版本号")
                   }

                }
            }
        }

        super.fillCompletionVariants(parameters, result)
    }

}