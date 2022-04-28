package shop.itbug.fluttercheckversionx.fix

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType


/**
 * 存在新版本的快速修复
 */
class NewVersinFix(
    psiElement: PsiElement,
    private val newVersion: String,
    val fixed: ()->Unit
) : LocalQuickFixOnPsiElement(psiElement) {


    override fun getFamilyName(): String {
        return "Repair $newVersion"
    }


    override fun getText(): String {
        return familyName;
    }

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val factory = JavaPsiFacade.getInstance(project).elementFactory
        val psiExpression =
            factory.createDummyHolder(
                "^$newVersion", IElementType(
                    "text",
                    Language.findLanguageByID("yaml")
                ), null
            )
        startElement.replace(psiExpression)
        fixed()
    }

}