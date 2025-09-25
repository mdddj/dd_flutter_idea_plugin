package shop.itbug.fluttercheckversionx.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.util.YamlExtends

/**
 *
 * yaml文件
 * 检查包是否已经停止更新
 *
 */
class PubspecPackageDiscontinuedAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val yamlEx = YamlExtends(element)
        if (element !is YAMLKeyValueImpl) return
        val keyEle = element.node.findChildByType(YAMLTokenTypes.SCALAR_KEY) ?: return
        val pubData = yamlEx.tryGetPackageInfo() ?: return
        if (pubData.isDiscontinued == true) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "FlutterX: This package has been marked to stop updating. Please replace it with another package "
            )
                .range(keyEle)
                .highlightType(ProblemHighlightType.WARNING)
                .create()
        }

    }
}