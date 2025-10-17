package shop.itbug.fluttercheckversionx.manager

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartImportStatementImpl
import com.jetbrains.lang.dart.psi.impl.DartMetadataImpl
import shop.itbug.fluttercheckversionx.actions.freezed.generateConstructorString
import shop.itbug.fluttercheckversionx.util.*


typealias IfExistsFun = (element: PsiElement) -> Unit

fun DartClassDefinitionImpl.myManagerFun(): DartClassManager = DartClassManager(this)

/**
 * 对dart类的一些操作函数
 */
class DartClassManager(val className: String, private val psiElement: DartClassDefinitionImpl) {

    private val frs = listOf("freezed", "Freezed", "unfreezed")

    constructor(psiElement: DartClassDefinitionImpl) : this(
        className = psiElement.componentName.name ?: "",
        psiElement = psiElement
    )


    //是否为抽象类
    fun isAbstract(): Boolean {
        val abs = psiElement.node.findChildByType(DartTokenTypes.ABSTRACT)
        return abs != null
    }

    //是否为 sealed
    fun isSealed(): Boolean {
        val sealed = psiElement.node.findChildByType(DartTokenTypes.SEALED)
        return sealed != null
    }


    //是否为 freezed 3 的 class
    fun isFreezed3Class(): Boolean {
        return isSealed() || isAbstract()
    }


    /**
     * 初始化freezed 虚拟文件
     */
    private val psiFile = MyDartPsiElementUtil.genFreezedClass(
        project = psiElement.project,
        className = className,
        addConstructor = false,
        addFromJson = true,
        properties = "String? text"
    )

    /**
     * 添加freezed fromJson函数
     */
    fun addFreezedFromJsonConstructor(ifExists: IfExistsFun? = null) {
        val findFromJsonElement =
            findTypesByPsiElement<DartFactoryConstructorDeclarationImpl>().find { it.componentName?.text == "fromJson" }
        if (findFromJsonElement != null) {
            ifExists?.invoke(findFromJsonElement)
            return
        }
        psiElement.classBody?.classMembers?.let {
            findFromJsonTextByPsiFile()?.apply {
                WriteCommandAction.runWriteCommandAction(psiElement.project) {
                    psiElement.classBody?.classMembers?.add(this)
                    psiElement.reformatText()
                }
            }
        }
    }

    /**
     * 查找生成的列表
     */
    private fun <T : PsiElement> findTypeList(clazz: Class<T>): Collection<T> {
        return PsiTreeUtil.findChildrenOfType(psiFile, clazz)
    }

    /**
     * 所有构造函数
     */
    fun getAllFactoryConstructorList() =
        PsiTreeUtil.findChildrenOfType(psiElement, DartFactoryConstructorDeclarationImpl::class.java)

    /**
     * 查找fromJson函数
     */
    private fun findFromJsonTextByPsiFile(): DartFactoryConstructorDeclarationImpl? {
        val children = findTypeList(DartFactoryConstructorDeclarationImpl::class.java)
        if (children.size > 1) {
            return children.find { it.componentName?.text == "fromJson" }
        }
        return null
    }


    /**
     * 查找所有T类型的psi节点
     */
    private inline fun <reified T : PsiElement> findTypesByPsiElement(): List<T> {
        val list: List<PsiElement> = MyPsiElementUtil.findAllMatchingElements(psiElement) { _, p ->
            return@findAllMatchingElements p is T
        }
        return list.filterIsInstance<T>()
    }


    fun showHints(edit: Editor, psiElement: PsiElement, text: String) {
        val relative = psiElement.getRelativePoint(edit)
        JBPopupFactory.getInstance().createBalloonBuilder(JBLabel(text)).createBalloon()
            .show(relative, Balloon.Position.above)
    }

    fun findFreezedMetadata(): DartMetadataImpl? {
        return findTypesByPsiElement<DartMetadataImpl>().lastOrNull { frs.contains(it.referenceExpression.text) }
    }

    fun hasFreezeMetadata(): Boolean {
        val eles = findTypesByPsiElement<DartMetadataImpl>()
        val names = eles.map { it.referenceExpression.text }
        return names.any { it in frs }
    }

    /**
     * 添加part语句
     */
    fun addPartForSuffix(suffix: String, ifExists: IfExistsFun? = null) {
        val fileName = psiElement.getFileName()
        val part = MyDartPsiElementUtil.createDartPart("part '$fileName.$suffix.dart';", psiElement.project)
        if (part != null) {

            //判断是否存在,如果存在就不要重复添加了
            psiElement.containingFile.findPsiElementByText(part.text)?.apply {
                ifExists?.invoke(this)
                return
            }

            val importStatement =
                PsiTreeUtil.findChildrenOfType(psiElement.containingFile, DartImportStatementImpl::class.java)
            if (importStatement.isNotEmpty()) {
                WriteCommandAction.runWriteCommandAction(psiElement.project) {
                    importStatement.last().apply {
                        addAfter(part, this.lastChild)
                        psiElement.containingFile.reformatText()
                    }
                }
            } else {
                psiElement.containingFile.firstChild.apply {
                    this.runWriteCommandAction {
                        addBefore(part, this.lastChild)
                        psiElement.containingFile.reformatText()
                    }
                }
            }

        } else {
            psiElement.project.toastWithError("add Failed!")
        }
    }


    //构造转属性
    fun addFinalProperties(factoryElement: DartFactoryConstructorDeclarationImpl) {
        val project = factoryElement.project
        val factoryManager = factoryElement.manager()
        val args = factoryManager.getNamedFields
        if (args.isNotEmpty()) {
            val finalElements = args.mapNotNull { it.createFinalField(project) }
            val dh = DartPsiElementUtil.createDh(project)
            val classMembers = psiElement.classBody?.classMembers ?: return
            WriteCommandAction.runWriteCommandAction(project) {
                finalElements.forEach {
                    val newEle = classMembers.addBefore(it, factoryElement)
                    classMembers.addAfter(dh, newEle)
                }
            }
        }
    }

    //添加构造
    fun addSimpleFactory(factoryElement: DartFactoryConstructorDeclarationImpl) {
        val classMembers = psiElement.classBody?.classMembers ?: return
        val project = factoryElement.project
        val manager = factoryElement.manager()
        val args = manager.getPropertiesWrapper
        val clazz = args.generateConstructorString(manager.getClassName)
        val createPsi = DartPsiElementUtil.createMethodDeclaration(project, manager.getClassName, clazz)
        if (createPsi != null) {
            WriteCommandAction.runWriteCommandAction(project) {
                classMembers.addBefore(createPsi, factoryElement)
            }
        }
    }

    fun showChooseConstructorPopup(
        point: RelativePoint,
        onSelect: (item: DartFactoryConstructorDeclarationImpl) -> Unit
    ) {
        lateinit var popup: ListPopup
        val actionGroup = DefaultActionGroup()
        val factoryElements = runReadAction { getAllFactoryConstructorList() }
        if (factoryElements.isEmpty()) return
        factoryElements.forEach { item: DartFactoryConstructorDeclarationImpl ->
            val manager = item.manager()
            val text = runReadAction { manager.getClassName + "(${manager.allFieldList.size})" }
            actionGroup.add(object : DumbAwareAction(text) {
                override fun actionPerformed(e: AnActionEvent) {
                    popup.cancel()
                    onSelect.invoke(item)
                }
            })
        }
        popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Choose a constructor",
            actionGroup,
            SimpleDataContext.EMPTY_CONTEXT,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false
        )
        popup.show(point)
    }

}
