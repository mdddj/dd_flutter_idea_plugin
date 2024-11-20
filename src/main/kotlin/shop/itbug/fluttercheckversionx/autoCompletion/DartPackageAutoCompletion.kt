package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.model.psiElementString
import shop.itbug.fluttercheckversionx.services.PubService


/**
 * 自动补全包名
 * todo: 5.1.0 版本
 */
class DartPackageAutoCompletion : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(LeafPsiElement::class.java).withLanguage(YAMLLanguage.INSTANCE),
            Provider()
        )
    }
}


///提供者
private class Provider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val text = parameters.originalPosition?.text ?: ""
        val project = parameters.originalFile.project

        if (text.isNotBlank()) {

            val requestTask = object : Task.Backgroundable(project, "FlutterX Search: $text", false) {
                override fun run(indicator: ProgressIndicator) {
                    val packages = ApplicationUtil.runWithCheckCanceled({ PubService.search(text) }, indicator)
                    if (packages != null) {
                        val elements = packages.packages.map {
                            LookupElementBuilder.create(it.psiElementString()).withIcon(MyIcons.flutter)
                        }
                        println(elements)
                        ApplicationManager.getApplication().invokeLater {
                            result.withPrefixMatcher(text)
                            result.addAllElements(elements)
                            AutoPopupController.getInstance(project).scheduleAutoPopup(parameters.editor)
                        }

                    }
                }

            }
            requestTask.queue()
        }

    }

}