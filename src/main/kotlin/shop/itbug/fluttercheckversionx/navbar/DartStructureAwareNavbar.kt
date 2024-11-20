package shop.itbug.fluttercheckversionx.navbar

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.*

class DartStructureAwareNavbar : StructureAwareNavBarModelExtension() {
    override val language: Language
        get() = DartLanguage.INSTANCE

    override fun getPresentableText(`object`: Any?): String? {
        `object` ?: return null
        when (`object`) {
            is DartClassDefinitionImpl -> {
                return `object`.componentName.text
            }

            is DartMethodDeclarationImpl -> {
                return `object`.name
            }

            is DartExtensionDeclarationImpl -> {
                return PsiTreeUtil.findChildOfType(`object`, DartIdImpl::class.java)?.text
            }

            is DartGetterDeclarationImpl -> {
                return `object`.componentName.text
            }

            is DartFactoryConstructorDeclarationImpl -> {
                return `object`.componentName?.text
            }

            is DartNamedConstructorDeclarationImpl -> {
                return `object`.componentName?.text
            }

            is DartVarDeclarationListImpl -> {
                return `object`.varAccessDeclaration.componentName.text
            }

            is DartComponentNameImpl -> {
                return `object`.text
            }

            is LeafPsiElement -> {
                if (`object`.text == "class") {
                    return (`object`.parent as? DartClassDefinitionImpl)?.componentName?.text
                }
                return null
            }

            is DartFunctionDeclarationWithBodyOrNativeImpl -> {
                return `object`.componentName.text
            }

            is DartReturnTypeImpl -> {
                var t = (`object`.parent as? DartMethodDeclarationImpl)?.componentName?.text
                t = t ?: (`object`.parent as? DartGetterDeclarationImpl)?.componentName?.text
                return t
            }

            else -> return null
        }
    }

    override fun getParent(psiElement: PsiElement?): PsiElement? {
        if (psiElement is LeafPsiElement && psiElement.text == "class") {
            return psiElement.parent as? DartClassDefinitionImpl?
        } else if (psiElement is DartReturnTypeImpl) {
            return psiElement.parent as? DartMethodDeclarationImpl?
        }
        return super.getParent(psiElement)
    }
}