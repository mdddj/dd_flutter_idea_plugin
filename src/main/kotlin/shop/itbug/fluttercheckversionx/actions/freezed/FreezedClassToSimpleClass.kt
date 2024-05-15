package shop.itbug.fluttercheckversionx.actions.freezed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import shop.itbug.fluttercheckversionx.common.scroll
import shop.itbug.fluttercheckversionx.manager.DartDefaultFormalNamedParameterActionManager
import shop.itbug.fluttercheckversionx.manager.DartFactoryConstructorDeclarationImplManager
import shop.itbug.fluttercheckversionx.manager.constr_type_string
import shop.itbug.fluttercheckversionx.manager.type_string
import shop.itbug.fluttercheckversionx.widget.DartEditorTextPanel
import java.awt.Dimension
import javax.swing.JComponent


private data class SettingModel(var className: String = "", var generateClass: String = "")

/**
 * freezed 转成 简单的 class
 */
class FreezedClassToSimpleClass : AnAction() {


    override fun actionPerformed(e: AnActionEvent) {
        val factoryPsiElement = e.getFactoryPsiElement()!!
        e.project?.let {
            FreezedClassToSimpleClassDialog(it, factoryPsiElement).show()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.getFactoryPsiElement() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}


///生成对于的 class类
fun List<DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper>.generateClassString(className: String): String {
    val str = """
    class $className {${
        joinToString(
            separator = ";", postfix = ";"
        ) { "final ${it.type_string} ${it.name}".prependIndent("\n\t\t") }
    }$className({${joinToString(separator = ",", postfix = "".prependIndent("\n\t\t")) { it.constr_type_string }}});
    }
    """.trimIndent()
    println(str)
    return str
}

///转换的弹窗
private class FreezedClassToSimpleClassDialog(project: Project, val psiElement: DartFactoryConstructorDeclarationImpl) :
    MyDialogWrapper(project) {


    private val manager: DartFactoryConstructorDeclarationImplManager
        get() = DartFactoryConstructorDeclarationImplManager(psiElement)

    private val args: List<DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper> =
        manager.getPropertiesWrapper
    private val setting = SettingModel(
        className = manager.getClassName, generateClass = args.generateClassString(manager.getClassName)
    )
    private val editor = DartEditorTextPanel(project, setting.generateClass)

    init {

        super.init()
        super.setTitle("freezed to simple class")
    }

    override fun createCenterPanel(): JComponent {
        return Layout()
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(500, super.getPreferredSize().height)
    }


    ///布局
    private inner class Layout : BorderLayoutPanel() {



        init {
            addToCenter(editor.scroll().apply {
                this.maximumSize = Dimension(500, -1)
            })
            addToLeft(panel {
                row("class name") {
                    textField().bindText(setting::className)
                }
            })
        }
    }
}

