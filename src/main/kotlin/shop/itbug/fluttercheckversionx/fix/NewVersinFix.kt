package shop.itbug.fluttercheckversionx.fix

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import com.jetbrains.lang.dart.psi.DartPsiCompositeElement
import com.jetbrains.lang.dart.util.DartElementGenerator
import com.jetbrains.lang.dart.util.DartPsiImplUtil
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLPsiElement


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

       val newElement =  DartElementGenerator.createStatementFromText(project,"^$newVersion")

        newElement?.let {
            startElement.replace(it)
            fixed()
        }

    }

}