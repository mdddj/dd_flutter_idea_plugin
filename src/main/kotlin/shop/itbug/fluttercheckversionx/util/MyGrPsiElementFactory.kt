package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression

class MyGrPsiElementFactory(val project: Project) {


    fun createMethodCall(text: String): GrMethodCallExpression {
        val file = GroovyPsiElementFactory.getInstance(project)
            .createGroovyFile(text, true, null)
        return PsiTreeUtil.findChildOfType(file, GrMethodCallExpression::class.java)!!
    }
}

object GrPsiElementHelper {
    fun methodCallAddMethodCall(
        project: Project,
        originMethodCall: GrMethodCallExpression,
        addedMethodCall: List<GrMethodCallExpression>
    ) {
        val closableBlock = originMethodCall.closureArguments
        if (closableBlock.isEmpty()) {
            return
        }
        val lastBlock = closableBlock.last()
        val statements = lastBlock.statements
        val lastMethodEle = statements.last()
        WriteCommandAction.runWriteCommandAction(project) {
            addedMethodCall.forEach {
                lastBlock.addAfter(it, lastMethodEle)
            }
        }
    }
}