package shop.itbug.fluttercheckversionx.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.psi.impl.DartCallExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartReferenceExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.string

private val dartClass = arrayOf<String>("Image.asset", "ExtendedImage.asset")

/**
 * 检查 asset资产文件是否存在
 */
class DartAssetsAnnotator : Annotator {

    override fun annotate(element: PsiElement, p1: AnnotationHolder) {
        if (element !is DartCallExpressionImpl || element.firstChild !is DartReferenceExpressionImpl) {
            return
        }
        val callElement = element
        val dartReferenceElement = element.firstChild as DartReferenceExpressionImpl

        if (!dartClass.contains(dartReferenceElement.text)) {
            return
        }

        val args = callElement.arguments ?: return
        val argList = args.argumentList ?: return
        val assetStringElement = argList.firstChild as? DartStringLiteralExpressionImpl ?: return
        val assetUrl = assetStringElement.string ?: return
        val hasFile = MyFileUtil.hasFileInAssetPath(assetUrl, element.project)
        if (!hasFile) {
            p1.newAnnotation(HighlightSeverity.ERROR, "FlutterX: Unresolved assets path")
                .range(assetStringElement)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .create()
        }
    }
}