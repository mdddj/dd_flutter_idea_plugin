package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.io.File


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
                                        val clipboard: Clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
                                        clipboard.setContents(ss, null)
                                    }
                                sink.addInlineElement(
                                    filterIsInstanceWithName.endOffset, false, ins, false
                                )
                            }
                        }
                    }
                }

                /// Doc 类型的超级注解
                if (element is DartReferenceExpressionImpl) {
                    val resolve = element.reference?.resolve()
                    if (resolve != null) {
                        val firstChild = resolve.parent.firstChild
                        if (firstChild is DartMetadataImpl) {
                            val doc = firstChild.firstChild.nextSibling
                            if (doc != null && doc.text == "Doc") {
                                val children = doc.nextSibling.children.filterIsInstance<DartArgumentListImpl>()
                                if (children.isNotEmpty()) {
                                    val args = children[0].children.filterIsInstance<DartNamedArgumentImpl>() /// 参数列表
                                    for (arg in args) {
                                        if (arg.firstChild.text == "message") {
                                            val docMessage =
                                                (arg.lastChild as DartStringLiteralExpressionImpl).canonicalText.replace(
                                                    "\"",
                                                    ""
                                                ).replace("\'", "")
                                            val semicolonElement = element.parent.nextSibling;
                                            if (semicolonElement is LeafPsiElement) {
                                                sink.addInlineElement(
                                                    semicolonElement.endOffset,
                                                    false,
                                                    hintsInlayPresentationFactory.simpleText(docMessage, "") { _, _ ->
                                                        println("被点击了...")
                                                    },
                                                    false
                                                )

                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }

                }


                //资产图片的展示
                if (element.lastChild is DartReferenceExpressionImpl && settings.showImageIconInEditor) {
                    element.reference?.let {
                        val referenceValElement = it.resolve()?.parent?.parent
                        if (referenceValElement is DartVarDeclarationListImpl && referenceValElement.lastChild is DartVarInitImpl) {
                            val varInitElement = referenceValElement.lastChild
                            if (varInitElement.lastChild is DartStringLiteralExpressionImpl) {
                                val path = varInitElement.lastChild.text.replace("'", "").replace("\"", "")
                                hintsInlayPresentationFactory.getImageWithPath(file.project.basePath + File.separator + path,path)
                                    ?.let { it1 -> sink.addInlineElement(element.endOffset, false, it1, false) }
                            }
                        }
                    }


//                    editor.inlayModel.addBlockElement(
//                        EditorUtil.getPlainSpaceWidth(editor),
//                        false,
//                        true,
//                        1,
//                        object : EditorCustomElementRenderer {
//                            override fun calcWidthInPixels(inlay: Inlay<*>): Int {
//                                val lineNumber = editor.document.getLineNumber(element.startOffset)
//                                val lineStartOffset = editor.document.getLineStartOffset(lineNumber)
//                                val lineEndOffset = editor.document.getLineEndOffset(lineNumber)
//                                return lineEndOffset - lineStartOffset
//                            }
//
//                            override fun paint(
//                                inlay: Inlay<*>,
//                                g: Graphics2D,
//                                targetRegion: Rectangle2D,
//                                textAttributes: TextAttributes
//                            ) {
//                                g.drawString("hello world!",targetRegion.x.toFloat(),targetRegion.y.toFloat())
//                                super.paint(inlay, g, targetRegion, textAttributes)
//                            }
//                        }
//                    )
                }


                if(element is DartStringLiteralExpressionImpl) {
                    val path = element.text.replace("'", "").replace("\"", "")
                    hintsInlayPresentationFactory.getImageWithPath(file.project.basePath + File.separator + path,path)
                        ?.let { it1 -> sink.addInlineElement(element.endOffset, false, it1, false) }
                }

                return true
            }

        }
    }


    override fun createConfigurable(settings: GenerateAssetsClassConfigModel): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }

}
