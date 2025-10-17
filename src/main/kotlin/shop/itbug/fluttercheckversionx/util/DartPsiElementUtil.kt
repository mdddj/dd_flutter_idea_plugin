package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartMethodDeclaration
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartTypeImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import com.jetbrains.lang.dart.util.DartElementGenerator
import shop.itbug.fluttercheckversionx.manager.myManagerFun
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

    fun createDartVarDeclarationByText(project: Project, text: String): DartVarDeclarationListImpl? {
        val file = DartElementGenerator.createDummyFile(project, text)
        return PsiTreeUtil.findChildOfType(file, DartVarDeclarationListImpl::class.java)
    }


    fun createMethodDeclaration(project: Project, className: String, text: String): DartMethodDeclaration? {
        val file = DartElementGenerator.createDummyFile(
            project, """
            class $className {
                $text
            }
        """.trimIndent()
        )
        return PsiTreeUtil.findChildOfType(file, DartMethodDeclaration::class.java)
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

    fun getModels(list: List<DartVarDeclarationListImpl>): List<DartClassProperty> {
        return list.map { it.covertDartClassPropertyModel() }
    }


    fun findParentElementOfType(element: PsiElement, parentClass: Class<out PsiElement?>): PsiElement? {
        val parent = element.parent
        return if (parent == null || parentClass.isInstance(parent)) {
            parent
        } else {
            findParentElementOfType(parent, parentClass)
        }
    }

    //获取所有的 freezed class
    fun findAllFreezedClass(file: VirtualFile, project: Project): List<DartClassDefinitionImpl> {
        val file = PsiManager.getInstance(project).findFile(file) ?: return emptyList()
        val classList = PsiTreeUtil.findChildrenOfType(file, DartClassDefinitionImpl::class.java)
        if (classList.isEmpty()) return emptyList()
        return classList.filter { it.myManagerFun().hasFreezeMetadata() }
    }

    //获取所有不是 freezed 3.0的 class
    fun findAllFreezedClassNot3Version(file: VirtualFile, project: Project): List<DartClassDefinitionImpl> {
        val find = findAllFreezedClass(file, project)
        if (find.isEmpty()) return emptyList()

        return find.filter { it.myManagerFun().isFreezed3Class().not() }
    }


    fun createDh(project: Project): PsiElement {
        val statement = DartElementGenerator.createDummyFile(project, "var x = 1;")
        return PsiTreeUtil.findChildrenOfAnyType(statement, LeafPsiElement::class.java).last()
    }
}


