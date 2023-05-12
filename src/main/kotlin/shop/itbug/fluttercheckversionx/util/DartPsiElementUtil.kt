package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
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

fun DartClassDefinitionImpl.addAnnotation(name: String,project: Project):DartClassDefinitionImpl{
    DartPsiElementUtil.classAddAnnotation(this,name,project)
    return this
}

fun DartClassDefinitionImpl.addMixin(name: String,project: Project):DartClassDefinitionImpl{
    DartPsiElementUtil.classAddMixin(this,name, project)
    return this
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

    /**
     * 给class添加注解
     */
    fun classAddAnnotation(classElement: DartClassDefinitionImpl,name: String,project: Project) {
        val metadata = MyDartPsiElementUtil.generateDartMetadata(name, project)
        val generateSpace = MyDartPsiElementUtil.generateSpace(project)
        runWriteAction {
            classElement.addAfter(generateSpace,classElement.prevSibling)
            classElement.addAfter(metadata,classElement.prevSibling)
        }
    }

    fun classAddMixin(classElement: DartClassDefinitionImpl,name: String,project: Project) {
        val generateSpace = MyDartPsiElementUtil.generateSpace(project, " ")
        val generateMixins = MyDartPsiElementUtil.generateMixins(project, name)
        val nameElement = PsiTreeUtil.findChildOfType(classElement, DartComponentNameImpl::class.java)!!
        runWriteAction {
            nameElement.addBefore(generateSpace,classElement.nextSibling)
            nameElement.addBefore(generateMixins,classElement.nextSibling)
        }

    }


}