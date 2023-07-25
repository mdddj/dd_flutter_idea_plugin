package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
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
                    val nameComm = element.children.filterIsInstance<DartVarAccessDeclarationImpl>()
                    if (nameComm.isNotEmpty()) {
                        val filterIsInstanceWithName = nameComm[0]
                        val filterIsInstance =
                            filterIsInstanceWithName.children.filterIsInstance<DartComponentNameImpl>()[0]
                        filterIsInstance.parent.nextSibling ?: return true

                        val analysisGethover = DartAnalysisServerService.getInstance(file.project)
                            .analysis_getHover(file.virtualFile, filterIsInstance.textOffset)
                        if (analysisGethover.isNotEmpty()) {
                            val staticType = analysisGethover[0].staticType
                            analysisGethover[0].dartdoc
                            if (staticType != null) {
                                val ins =
                                    hintsInlayPresentationFactory.simpleText(staticType, "类型:$staticType") { _, _ ->
                                        val ss = StringSelection(staticType)
                                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                        clipboard.setContents(ss, null)
                                    }
                                sink.addInlineElement(
                                    filterIsInstanceWithName.endOffset, false, ins, false
                                )
                            }
                        }
                    }
                }


                //资产图片的展示
//                if (element.lastChild is DartReferenceExpressionImpl && settings.showImageIconInEditor) {
//                    element.reference?.let {
//                        val referenceValElement = it.resolve()?.parent?.parent
//                        if (referenceValElement is DartVarDeclarationListImpl && referenceValElement.lastChild is DartVarInitImpl) {
//                            val varInitElement = referenceValElement.lastChild
//                            if (varInitElement.lastChild is DartStringLiteralExpressionImpl) {
//                                val path = varInitElement.lastChild.text.replace("'", "").replace("\"", "")
//                                hintsInlayPresentationFactory.getImageWithPath(file.project.basePath + File.separator + path,path)
//                                    ?.let { it1 -> sink.addInlineElement(element.endOffset, false, it1, false) }
//                            }
//                        }
//                    }
//
//                }


//                if(element is DartStringLiteralExpressionImpl) {
//                    val path = element.text.replace("'", "").replace("\"", "")
//                    hintsInlayPresentationFactory.getImageWithPath(file.project.basePath + File.separator + path,path)
//                        ?.let { it1 -> sink.addInlineElement(element.endOffset, false, it1, false) }
//                }

                return true
            }

        }
    }


    override fun createConfigurable(settings: GenerateAssetsClassConfigModel): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }

}
