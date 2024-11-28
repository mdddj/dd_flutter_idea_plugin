package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PubService


/**
 * 自动补全包名
 * todo: 5.1.0 版本
 */
class DartPackageAutoCompletion : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(LeafPsiElement::class.java)
                .withParent(YAMLPlainTextImpl::class.java)
                .withParent(YAMLKeyValueImpl::class.java)
                .withLanguage(YAMLLanguage.INSTANCE),
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
        if (text.isNotBlank()) {
            val packages = ApplicationUtil.runWithCheckCanceled(
                {
                    val r = PubService.search(text)?.packages ?: emptyList()
                    return@runWithCheckCanceled PubService.findAllPluginInfo(r.map { it.`package` })
                },
                ProgressManager.getInstance().progressIndicator
            )
            if (packages != null) {
                val elements = packages.map {
                    val info = it.model
                    LookupElementBuilder.create("${info.name}: ^${info.latest.version}").withIcon(MyIcons.flutter)
                        .withTailText(" " + info.formatTime(), true)
                        .withTypeText(it.score.likeCount.toString(), MyIcons.score, true)
                }
                result.addAllElements(elements)
                result.runRemainingContributors(parameters, false)
            }
        }

    }

}