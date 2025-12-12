package shop.itbug.flutterx.actions.freezed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import shop.itbug.flutterx.common.MyDialogWrapper
import shop.itbug.flutterx.manager.*
import shop.itbug.flutterx.widget.DartEditorTextPanel
import java.awt.Dimension
import javax.swing.JComponent


data class SettingModel(var className: String = "", var generateClass: String = "")

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
    return str
}

fun List<DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper>.generateConstructorString(className: String): String {
    val str = """
$className({${joinToString(separator = ",\n\t", postfix = "".prependIndent("\n")) { it.constr_type_string_use_default_value }}});
    """.trimIndent()
    return str
}

private class FreezedClassToSimpleClassDialog(
    project: Project,
    val psiElement: DartFactoryConstructorDeclarationImpl
) : MyDialogWrapper(project) {

    private val manager: DartFactoryConstructorDeclarationImplManager
        get() = DartFactoryConstructorDeclarationImplManager(psiElement)

    private val args: List<DartDefaultFormalNamedParameterActionManager.MyPropertiesWrapper> =
        manager.getPropertiesWrapper

    private val setting = SettingModel(
        className = manager.getClassName,
        generateClass = args.generateClassString(manager.getClassName)
    )

    private val editor: DartEditorTextPanel by lazy { DartEditorTextPanel(project, setting.generateClass) }  // 在这里初始化

    init {
        super.init()
        super.setTitle("Freezed To Simple Class")
    }

    override fun createCenterPanel(): JComponent {
        return object : BorderLayoutPanel() {
            init {
                addToCenter(editor)
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(500, super.getPreferredSize().height)
    }
}
