package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLDocumentImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.model.PubPackageInfo
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.util.YamlExtends


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
                .withSuperParent(2, YAMLBlockMappingImpl::class.java)
                .withSuperParent(3, YAMLKeyValueImpl::class.java)
                .withSuperParent(5, YAMLDocumentImpl::class.java)
                .withLanguage(YAMLLanguage.INSTANCE),
            Provider()
        )

    }


}



private class VersionProvider : CompletionProvider<CompletionParameters>() {
    private val logger = thisLogger()
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val text = result.prefixMatcher.prefix
        logger.info("前缀:${text}")
        if (text.startsWith("^")) {
            val pluginNameEle = parameters.position.parent.parent.firstChild
            if (pluginNameEle.node == YAMLTokenTypes.SCALAR_KEY) {
                val yamlExt = YamlExtends(pluginNameEle)
                val pluginName = yamlExt.getDartPluginNameAndVersion()?.name ?: return
                ProgressManager.checkCanceled()
                ApplicationUtil.runWithCheckCanceled({
                    val versions = PubService.getPackageVersions(pluginName)
                    logger.info(versions.toString())
                }, ProgressManager.getGlobalProgressIndicator())
            }

        }
    }
    // result.addElement(LookupElementBuilder.create(it))

}

///dart package 提供者
private class Provider : CompletionProvider<CompletionParameters>() {
    private val logger = thisLogger()
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val text = result.prefixMatcher.prefix
        logger.info("yaml 搜索包: $text")
        if (text.isNotBlank()) {
            ProgressManager.checkCanceled()
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

    private fun addItemResult(infoModel: PubPackageInfo,result:  CompletionResultSet) {
        val info = infoModel.model
        val score = infoModel.score
        val element = LookupElementBuilder.create("${info.name}: ^${info.latest.version}").withIcon(MyIcons.flutter)
            .withTailText(" " + info.formatTime(), true)
            .withTypeText(score.likeCount.toString(), MyIcons.score, true)
        result.addElement(element)
    }

}