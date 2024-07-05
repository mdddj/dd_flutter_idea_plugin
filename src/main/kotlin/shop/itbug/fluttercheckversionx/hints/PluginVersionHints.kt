package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.PubService

/// 自动加载版本号
class PluginVersionHints : CompletionContributor() {

    //    填充完成事件
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val currLinePluginName = parameters.originalPosition?.parent?.parent?.firstChild?.text
        val project = parameters.originalPosition?.project
        if (currLinePluginName != null && project != null) {
            val task = object : Task.Backgroundable(
                project,
                PluginBundle.get("fill.completion.task.title") + ":${currLinePluginName}"
            ) {
                override fun run(indicator: ProgressIndicator) {
                    val r = ApplicationUtil.runWithCheckCanceled(
                        { PubService.getPackageVersions(currLinePluginName) },
                        indicator
                    )
                    r?.versions?.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }
            }
            task.queue()
        }
        super.fillCompletionVariants(parameters, result)
    }

}