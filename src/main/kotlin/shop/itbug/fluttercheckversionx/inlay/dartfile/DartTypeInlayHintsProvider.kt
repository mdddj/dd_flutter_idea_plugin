package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import com.intellij.psi.util.elementType
import com.intellij.psi.util.nextLeaf
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.components.DialogManager
import com.jetbrains.lang.dart.DartElementType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.dialog.ExampleModelDialog
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable
import shop.itbug.fluttercheckversionx.socket.service.AppService
import java.awt.Point
import java.awt.event.MouseEvent

class DartTypeInlayHintsProvider : InlayHintsProvider<DartTypeInlayHintsProvider.Setting> {


    data class Setting(private val enable: Boolean)

    override val key: SettingsKey<Setting>
        get() = SettingsKey("dart.type.inlay.hints.provider")
    override val name: String
        get() = "dart.type.inlay.hints.provider"
    override val previewText: String
        get() = """
            var a = 0;
            final b = false;
            const c = "hello world";
        """.trimIndent()

    override fun createSettings(): Setting {
        return Setting(true)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Setting,
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
                            if (staticType != null) {
                                sink.addInlineElement(
                                    filterIsInstanceWithName.endOffset, false,
                                    hintsInlayPresentationFactory.simpleText(staticType, "??????:$staticType"), false
                                )
                            }
                        }
                    }
                }

                /// Doc ?????????????????????
                if (element is DartReferenceExpressionImpl) {
                    val resolve = element.reference?.resolve()
                    if (resolve != null) {
                        val firstChild = resolve.parent.firstChild
                        if (firstChild is DartMetadataImpl) {
                            val doc = firstChild.firstChild.nextSibling
                            if (doc != null && doc.text == "Doc") {
                                val children = doc.nextSibling.children.filterIsInstance<DartArgumentListImpl>()
                                if (children.isNotEmpty()) {
                                    val args = children[0].children.filterIsInstance<DartNamedArgumentImpl>() /// ????????????
                                    for (arg in args) {
                                        if (arg.firstChild.text == "message") {
//                                            val docMessage = arg.lastChild.text.replace("\"","")
                                            val docMessage =
                                                (arg.lastChild as DartStringLiteralExpressionImpl).canonicalText.replace(
                                                    "\"",
                                                    ""
                                                ).replace("\'", "")

                                            //???????????????";"
                                            val semicolonElement = element.parent.nextSibling;
                                            ///?????????????????????
                                            if (semicolonElement is LeafPsiElement) {
                                                sink.addInlineElement(
                                                    semicolonElement.endOffset,
                                                    false,
                                                    hintsInlayPresentationFactory.simpleText(docMessage, ""),
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


                if (element is DartReferenceExpressionImpl) {
                    val models = service<AppService>().examples
                    val strKeys = models.map { it.label }
                    if (strKeys.contains(element.text)) {
                        sink.addInlineElement(
                            element.endOffset,
                            false,
                            hintsInlayPresentationFactory.simpleTextWithClick("????????????", "??????????????????"
                            ) { event, translated -> ExampleModelDialog(project = element.project,
                            exampleModel = models.first { it.label == element.text }).show() },
                            false
                        )
                    }
                }

                return true
            }

        }
    }


    override fun createConfigurable(settings: Setting): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }

    override val group: InlayGroup
        get() = InlayGroup.TYPES_GROUP
}