package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression

class MyKotlinPsiElementFactory(val project: Project) {

    fun createCallExpression(text: String): KtCallExpression {
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "test.kts",
            KotlinFileType.INSTANCE,
            text
        )
        val find = PsiTreeUtil.findChildOfType(file, KtCallExpression::class.java)
        return find!!
    }
}