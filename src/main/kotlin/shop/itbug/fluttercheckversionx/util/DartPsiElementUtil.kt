package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartTypeImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import shop.itbug.fluttercheckversionx.model.DartClassProperty
import shop.itbug.fluttercheckversionx.model.covertDartClassPropertyModel

/**
 * 获取类节点
 */
fun AnActionEvent.getDartClassDefinition(): DartClassDefinitionImpl? {
    val psiElement = this.getData(CommonDataKeys.PSI_ELEMENT)
    psiElement?.let {
        val findFirstParent = PsiTreeUtil.findFirstParent(psiElement) { it is DartClassDefinitionImpl }
        if (findFirstParent != null) {
            return findFirstParent as DartClassDefinitionImpl
        }
    }
    return null
}


object DartPsiElementUtil {

    /**
     * 查找属性列表
     */
    fun getClassProperties(classElement: DartClassDefinitionImpl): List<DartVarDeclarationListImpl> {
        val findChildrenOfType = PsiTreeUtil.findChildrenOfType(classElement, DartVarDeclarationListImpl::class.java)
        return findChildrenOfType.toList()
    }

    /**
     * 获取属性名称
     */
    fun getNameWithVar(element: DartVarDeclarationListImpl): String {
        return PsiTreeUtil.findChildOfType(element, DartComponentNameImpl::class.java)?.text ?: ""
    }

    /**
     * 获取属性类型
     */
    fun getTypeWithVar(element: DartVarDeclarationListImpl): String {
        return PsiTreeUtil.findChildOfType(element, DartTypeImpl::class.java)?.text ?: ""
    }

    /**
     * 判断属性是否可空
     * @return true - 可空 false 不可空
     */
    fun getTypeIsNonNull(element: DartVarDeclarationListImpl): Boolean {
        return getTypeWithVar(element).findLast { it == '?' } != null
    }

    fun getModels(list: List<DartVarDeclarationListImpl>) : List<DartClassProperty> {
       return list.map { it.covertDartClassPropertyModel() }
    }
}