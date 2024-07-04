package shop.itbug.fluttercheckversionx.manager

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.components.JBLabel
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartImportStatementImpl
import com.jetbrains.lang.dart.psi.impl.DartMetadataImpl
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
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

    private val project = psiElement.project

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

    /**
     * 查找mata data psi 节点
     */
    fun findMataDataByText(text: String): DartMetadataImpl? {
        return findTypesByPsiElement<DartMetadataImpl>().find { it.referenceExpression.text == text }
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


    /**
     * //Todo v1.6.0 自动添加Hive注解
     **/
    fun generateHiveMate() {
        val constructor = findTypesByPsiElement<DartFactoryConstructorDeclarationImpl>().filter {
            val manager = it.manager()
            return@filter manager.hasProperties() && manager.hasHiveMate().not()
        }
        if (constructor.isNotEmpty()) {
            val popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "Choose One", DefaultActionGroup().apply {
                    constructor.forEach { ele ->
                        val name = ele.componentNameList.joinToString(".") { it.text }
                        add(object : MyDumbAwareAction({ name }) {
                            override fun actionPerformed(e: AnActionEvent) {

                            }
                        })
                    }
                },
                DataContext.EMPTY_CONTEXT, JBPopupFactory.ActionSelectionAid.MNEMONICS, true
            )
            popup.showCenteredInCurrentWindow(project)
        }
    }

}
