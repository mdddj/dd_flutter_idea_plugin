package shop.itbug.fluttercheckversionx.fix

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl


/**
 * 存在新版本的快速修复
 */
class NewVersinFix(
    psiElement: PsiElement,
    private val newVersion: String,
) : LocalQuickFixOnPsiElement(psiElement) {


    override fun getFamilyName(): String {
        return " 替换为最新版:$newVersion"
    }


    override fun getText(): String {
        return familyName
    }

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val pluginName = (startElement as YAMLKeyValueImpl).keyText
        val newElement = YAMLElementGenerator.getInstance(project).createYamlKeyValue(pluginName, newVersion)
        startElement.replace(newElement)
    }

}
