package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
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

        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(LeafPsiElement::class.java)
                .withParent(YAMLPlainTextImpl::class.java)
                .withSuperParent(2, YAMLKeyValueImpl::class.java)
                .withSuperParent(3, YAMLBlockMappingImpl::class.java)
                .withSuperParent(4, YAMLKeyValueImpl::class.java),
            VersionProvider()
        )

    }


}


private class VersionProvider : CompletionProvider<CompletionParameters>() {
    init {
        println("版本补全进来了..")
    }

    private val logger = thisLogger()
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val pluginNameEle = parameters.position.parent.parent.firstChild
        if (pluginNameEle.elementType == YAMLTokenTypes.SCALAR_KEY) {
            val yamlExt = YamlExtends(pluginNameEle.parent)
            val pluginName = yamlExt.getDartPluginNameAndVersion()?.name ?: return
            ProgressManager.checkCanceled()
            val versions = ApplicationUtil.runWithCheckCanceled({
                val versions = PubService.getPackageVersions(pluginName)
                logger.info(versions.toString())
                versions
            }, ProgressManager.getGlobalProgressIndicator())
            logger.info("versions: $versions")
            versions?.versions?.reversed()?.toList()?.forEach {
                result
                    .addElement(LookupElementBuilder.create(it).withIcon(MyIcons.flutter))
            }
            result.runRemainingContributors(parameters, false)
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

    private fun addItemResult(infoModel: PubPackageInfo, result: CompletionResultSet) {
        val info = infoModel.model
        val score = infoModel.score
        val element = LookupElementBuilder.create("${info.name}: ^${info.latest.version}").withIcon(MyIcons.flutter)
            .withTailText(" " + info.formatTime(), true)
            .withTypeText(score.likeCount.toString(), MyIcons.score, true)
        result.addElement(element)
    }

}