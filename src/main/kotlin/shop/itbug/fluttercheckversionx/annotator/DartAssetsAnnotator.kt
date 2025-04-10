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

private val dartClass = arrayOf("Image.asset", "ExtendedImage.asset", "SvgPicture.asset")

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
        if (assetUrl.isBlank()) return
        if (hasPackageAttr(element)) return
        if (isPackageAssets(assetUrl)) return //判断是不是第三方包里面的资产图片
        val hasFile = MyFileUtil.hasFileInAssetPath(assetUrl, element.project)
        if (!hasFile) {
            p1.newAnnotation(HighlightSeverity.WARNING, "FlutterX: Unresolved assets path")
                .range(assetStringElement)
                .highlightType(ProblemHighlightType.GENERIC_ERROR)
                .create()
        }
    }

    //检测使用使用的第三方包里面的资产图片
    fun isPackageAssets(url: String): Boolean {
        return url.startsWith("package:") || url.startsWith("packages/")
    }

    //检测是不是有 package属性
    fun hasPackageAttr(element: DartCallExpressionImpl): Boolean {
        val al = element.arguments?.argumentList?.namedArgumentList ?: return false
        return al.any { it.parameterReferenceExpression.text == "package" }
    }
}