package shop.itbug.fluttercheckversionx.actions.internal

import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.psi.impl.DartReturnStatementImpl

/**
 * dart psitree 工具类
 */
object DartPsiTreeUtil {


    fun findAncestor(element: PsiElement?, levelsUp: Int): PsiElement? {
        var currentElement = element
        var i = 0
        while (i < levelsUp && currentElement != null) {
            currentElement = currentElement.parent
            i++
        }
        return currentElement
    }


    fun findReturn(element: PsiElement?): DartReturnStatementImpl? {
        val ele = findAncestor(element, 3)
        if (ele is DartReturnStatementImpl) {
            return ele
        }
        val ele2 = findAncestor(element, 4)
        if (ele2 is DartReturnStatementImpl) {
            return ele2
        }
        return null
    }

}