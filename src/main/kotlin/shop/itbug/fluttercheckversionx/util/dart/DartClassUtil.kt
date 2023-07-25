package shop.itbug.fluttercheckversionx.util.dart

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartMetadataImpl

object DartClassUtil {

    /**
     * 判断某个类是否有[name]这个注解
     */
    fun hasMetadata(element:PsiElement, name: String):Boolean {
        if(element is DartClassDefinitionImpl) {
            val metadataList = PsiTreeUtil.findChildrenOfType(element, DartMetadataImpl::class.java) //查找注解列表
            if(metadataList.isNotEmpty()) {
                return metadataList.find { it.referenceExpression.text == name } != null
            }
        }
        return false
    }
}