package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer
import com.jetbrains.lang.dart.psi.impl.DartPatternFieldImpl
import com.jetbrains.lang.dart.psi.impl.DartSimpleFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartVarAccessDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartVariablePatternImpl
import shop.itbug.fluttercheckversionx.document.getDartElementType


private typealias GetPsiElementPosition = (type: String, element: PsiElement) -> Pair<Int, String>

/**
 * 新版dart类型,性能有增加
 */
class DartTypeInlayProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {

                if (element is DartVarAccessDeclarationImpl) {
                    if (element.type == null) {
                        sink.addDartTypeInlay(element.componentName, editor) { type, ele ->
                            Pair(ele.textRange.endOffset, ":$type")
                        }
                    }
                }

                //括号内
                if (element is DartSimpleFormalParameterImpl) {
                    if (element.type == null) {
                        sink.addDartTypeInlay(element.componentName, editor) { type, ele ->
                            Pair(ele.textRange.startOffset, "$type:")
                        }
                    }
                }

                //dart3.0++ `final text`
                if (element is DartVariablePatternImpl) {
                    if (element.type == null) {
                        sink.addDartTypeInlay(element.referenceExpression, editor) { type, ele ->
                            Pair(ele.textRange.startOffset, "$type:")
                        }
                    }
                }

                if (element is DartPatternFieldImpl) {
                    val hasType = element.variablePattern != null
                    val field = element.constantPattern
                    if (!hasType && field != null) {
                        sink.addDartTypeInlay(element, editor) { type, ele ->
                            Pair(ele.textRange.startOffset, "$type:")
                        }
                    }
                }
            }

        }
    }


    //添加dart类型
    private fun InlayTreeSink.addDartTypeInlay(element: PsiElement, editor: Editor, position: GetPsiElementPosition) {

        val elementType = element.getDartElementType()
        val htmlTip =
            HtmlChunk.div()
                .children(
                    HtmlChunk.text("Ctrl/Cmd").bold().italic().code(),
                    HtmlChunk.text("Try navigation jump")
                )
                .toString()
        if (elementType != null) {
            val (offset, text) = position(elementType, element)
            addPresentation(
                InlineInlayPosition(offset, false),
                null,
                htmlTip,
                HintFormat.default
            ) {
                text(
                    text, InlayActionData(
                        PsiPointerInlayActionPayload(element.createSmartPointer()),
                        "dartTypeInlayProviderId"
                    )
                )
            }
        }

    }


}

private class MyRender : EditorCustomElementRenderer {
    override fun calcWidthInPixels(p0: Inlay<*>): Int {
        TODO("Not yet implemented")
    }

}