package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.lang.dart.psi.impl.DartPatternFieldImpl
import com.jetbrains.lang.dart.psi.impl.DartSimpleFormalParameterImpl
import com.jetbrains.lang.dart.psi.impl.DartVarAccessDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartVariablePatternImpl
import shop.itbug.fluttercheckversionx.document.getDartElementType

/**
 * 新版dart类型,性能有增加
 */
class DartTypeInlayProvider : InlayHintsProvider {

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {

                if (element is DartVarAccessDeclarationImpl) {
                    if (element.type == null) {
                        sink.addDartTypeInlay(element.componentName)
                    }
                }

                if (element is DartSimpleFormalParameterImpl) {
                    if (element.type == null) {
                        sink.addDartTypeInlay(element.componentName)
                    }
                }

                //dart3.0++ `final text`
                if (element is DartVariablePatternImpl) {
                    if (element.type == null) {
                        sink.addDartTypeInlay(element.referenceExpression)
                    }
                }

                if (element is DartPatternFieldImpl) {
                    val hasType = element.variablePattern != null
                    val field = element.constantPattern
                    if (!hasType && field != null) {
                        sink.addDartTypeInlay(element)
                    }
                }
            }

        }
    }


    //添加dart类型
    private fun InlayTreeSink.addDartTypeInlay(element: PsiElement) {
        val elementType = element.getDartElementType()
        if (elementType != null) {
            addPresentation(
                InlineInlayPosition(element.textRange.startOffset, false),
                null,
                null,
                HintFormat.default.withHorizontalMargin(HintMarginPadding.OnlyPadding)
                    .withFontSize(HintFontSize.ABitSmallerThanInEditor)
                    .withColorKind(HintColorKind.TextWithoutBackground)
            ) {
                text("$elementType")
            }
        }

    }
}