package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.DartPatternFieldImpl
import com.jetbrains.lang.dart.psi.impl.DartSimpleFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartVarAccessDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection


class DartTypeInlayHintsProvider : InlayHintsProvider<GenerateAssetsClassConfigModel> {


    override val key: SettingsKey<GenerateAssetsClassConfigModel>
        get() = SettingsKey("dart.type.inlay.hints.provider")
    override val name: String
        get() = "dart.type.inlay.hints.provider"
    override val previewText: String
        get() = """
            var a = 0;
            final b = false;
        """.trimIndent()


    override fun createSettings(): GenerateAssetsClassConfigModel {
        return GenerateAssetsClassConfig.getGenerateAssetsSetting()
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: GenerateAssetsClassConfigModel,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val hintsInlayPresentationFactory = HintsInlayPresentationFactory(factory)
                if (element is DartVarDeclarationListImpl) {
                    val dartVar = PsiTreeUtil.findChildOfType(element, DartVarAccessDeclarationImpl::class.java)
                    if (dartVar != null) {
                        val comName = dartVar.componentName
                        val type = dartVar.type
                        if (type != null) {
                            return true //如果有类型就不显示了
                        }
                        val result = DartAnalysisServerService.getInstance(file.project)
                            .analysis_getHover(file.virtualFile, comName.textOffset)
                        if (result.isNotEmpty()) {
                            val staticType = result[0].staticType
                            result[0].dartdoc
                            if (staticType != null) {
                                val ins =
                                    hintsInlayPresentationFactory.simpleText(" :$staticType", staticType) { _, _ ->
                                        val ss = StringSelection(staticType)
                                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                        clipboard.setContents(ss, null)
                                    }
                                sink.addInlineElement(
                                    comName.textRange.endOffset, false, ins, false
                                )
                            }
                        }
                    }
                }

                if (element is DartSimpleFormalParameterImpl) {
                    if (element.type == null) {
                        val result = DartAnalysisServerService.getInstance(file.project)
                            .analysis_getHover(file.virtualFile, element.textOffset)
                        if (result.isNotEmpty()) {
                            val staticType = result[0].staticType
                            if (staticType != null) {
                                val ins =
                                    hintsInlayPresentationFactory.simpleText("$staticType: ", staticType) { _, _ ->
                                        val ss = StringSelection(staticType)
                                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                        clipboard.setContents(ss, null)
                                    }
                                sink.addInlineElement(
                                    element.textRange.startOffset, false, ins, false
                                )
                            }
                        }
                    }
                }

                if (element is DartPatternFieldImpl) {
                    val hasType = element.variablePattern != null
                    val field = element.constantPattern
                    if (!hasType && field != null) {
                        //添加类型
                        val result = DartAnalysisServerService.getInstance(file.project)
                            .analysis_getHover(file.virtualFile, field.textOffset)
                        if (result.isNotEmpty()) {
                            val staticType = result[0].staticType
                            if (staticType != null) {
                                val ins = hintsInlayPresentationFactory.simpleText(staticType, staticType) { _, _ ->

                                }
                                sink.addInlineElement(file.textRange.startOffset, false, ins, false)
                            }
                        }
                    }
                }
                return true
            }

        }
    }


    override fun createConfigurable(settings: GenerateAssetsClassConfigModel): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }

}
