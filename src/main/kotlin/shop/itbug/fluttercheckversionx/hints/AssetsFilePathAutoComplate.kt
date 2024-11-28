package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.FlutterAssetsService
import shop.itbug.fluttercheckversionx.services.PluginStateService
import shop.itbug.fluttercheckversionx.socket.formatSize
import shop.itbug.fluttercheckversionx.util.SwingUtil
import java.io.File


class FlutterAssetsStartHandle : ProjectActivity, DumbAware {

    override suspend fun execute(project: Project) {
        if (project.isDisposed) {
            return
        }
        val setting: AppStateModel = PluginStateService.getInstance().state ?: AppStateModel()
        if (setting.assetsScanEnable) {
            FlutterAssetsService.getInstance(project).init(setting.assetScanFolderName)
        }

    }
}

/**
 * 资源文件路径自动补全
 */
class AssetsFilePathAutoComplete : CompletionContributor() {
    private var setting: AppStateModel = PluginStateService.getInstance().state ?: AppStateModel()

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(LeafPsiElement::class.java)
                .withParent(DartStringLiteralExpressionImpl::class.java)
                .withLanguage(DartLanguage.INSTANCE),
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
        val element = parameters.originalPosition
        val project = parameters.editor.project
        val text = element?.text ?: ""
        if (text.startsWith(setting.assetCompilationTriggerString)) {
            project?.let {
                FlutterAssetsService.getInstance(it).allAssets().forEach { filePath ->
                    result
                        .addElement(
                            LookupElementBuilder.create(filePath.text)
                                .withIcon(
                                    SwingUtil.fileToIcon(File(filePath.file.path)) ?: MyIcons.flutter
                                ).withTypeText(formatSize(filePath.file.length))
                        )
                }
            }
        }
    }


}
