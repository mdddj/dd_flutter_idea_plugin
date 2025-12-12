package shop.itbug.flutterx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.jetbrains.lang.dart.psi.impl.DartMethodDeclarationImpl
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.manager.FieldToFreezedConfig
import shop.itbug.flutterx.manager.generateFreezedClass
import shop.itbug.flutterx.manager.myManager
import shop.itbug.flutterx.widget.DartEditorTextPanel
import java.awt.Dimension
import javax.swing.JComponent


//简单对象转freezed
class SimpleClassToFreezedActionDialog(project: Project, psiElement: DartMethodDeclarationImpl) :
    DialogWrapper(project, true) {

    private val manager = psiElement.myManager
    private val tab = JBTabbedPane()

    init {
        super.init()
        title = PluginBundle.get("simple_class_to_freezed_object_f1")
        tab.add(PluginBundle.get("preview_title"), DartEditorTextPanel(project, manager.generateFreezedClass()))
        tab.add(
            PluginBundle.get("preview_title") + " (Set default value)",
            DartEditorTextPanel(project, manager.generateFreezedClass(FieldToFreezedConfig(useDefault = true)))
        )
        tab.add(
            PluginBundle.get("preview_title") + " (Use underline naming)",
            DartEditorTextPanel(
                project,
                manager.generateFreezedClass(FieldToFreezedConfig(useDefault = false, useCamelCaseName = true))
            )
        )
        tab.add(
            PluginBundle.get("preview_title") + " (Use underline naming and set default value)",
            DartEditorTextPanel(
                project,
                manager.generateFreezedClass(FieldToFreezedConfig(useDefault = true, useCamelCaseName = true))
            )
        )
    }

    override fun createCenterPanel(): JComponent {
        return JBScrollPane(tab)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(500, super.getPreferredSize().height)
    }
}

///