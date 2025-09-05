package shop.itbug.fluttercheckversionx.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.tools.YAML_DART_PACKAGE_INFO_KEY

/**
 *
 * yaml文件
 * 检查包是否已经停止更新
 *
 */
class PubspecPackageDiscontinuedAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val project = element.project
        if (element !is YAMLKeyValueImpl) return
        val keyEle = element.node.findChildByType(YAMLTokenTypes.SCALAR_KEY) ?: return
        val pluginName = keyEle.text
        val psiFile = PsiManager.getInstance(project).findFile(element.containingFile?.virtualFile ?: return) ?: return
        val details = psiFile.getUserData(YAML_DART_PACKAGE_INFO_KEY) ?: return
        val pluginInfo = details.find { it.name == pluginName } ?: return
        val pubData = pluginInfo.pubData ?: return
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