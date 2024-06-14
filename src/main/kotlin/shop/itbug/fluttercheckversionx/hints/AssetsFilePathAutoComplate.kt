package shop.itbug.fluttercheckversionx.hints

import FlutterAssetsService
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService


class FlutterAssetsStartHandle : ProjectActivity, DumbAware {

    override suspend fun execute(project: Project) {
        if (project.isDisposed) {
            return
        }
        val setting: AppStateModel = PluginStateService.getInstance().state ?: AppStateModel()
        FlutterAssetsService.getInstance(project).init(setting.assetScanFolderName)
    }
}

/**
 * 资源文件路径自动补全
 */
class AssetsFilePathAutoComplete : CompletionContributor() {
    private var setting: AppStateModel = PluginStateService.getInstance().state ?: AppStateModel()

    init {
        println(setting)
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(DartLanguage.INSTANCE)
                .withParents(DartStringLiteralExpressionImpl::class.java)
                .withText(PlatformPatterns.string().startsWith(setting.assetCompilationTriggerString)),
            AssetsFilePathAutoCompleteProvider(setting)
        )
    }


}


/**
 * 资产文件自动补全
 */
class AssetsFilePathAutoCompleteProvider(val setting: AppStateModel) : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        parameters.editor.project?.let {
            FlutterAssetsService.getInstance(it).allAssets().forEach { filePath ->
                result.addElement(LookupElementBuilder.create(filePath).withIcon(MyIcons.flutter))
            }
        }
    }


}
