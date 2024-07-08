package shop.itbug.fluttercheckversionx.dialog.macro

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.dialog.validParseToFreezed
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.tools.DartMarcoClassConfig
import shop.itbug.fluttercheckversionx.tools.MyChildObject
import shop.itbug.fluttercheckversionx.tools.MyJsonParseTool
import shop.itbug.fluttercheckversionx.tools.generateDartMacro
import shop.itbug.fluttercheckversionx.widget.DartEditorTextPanel
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import java.awt.Dimension
import javax.swing.JComponent

private val testJson = """
    {
        "hello":"world",
        "user": {
            "name":"梁典典"
        },
        "array":[
            1,2,3
        ],
        "string_array":[
            "hello","dart"
        ],
        "obj_arr":[
            {
                "test": true
            }
        ]
    }
""".trimIndent()

/// 输入json转dart macro 宏
class EnterJsonToDartMacroDialog(val project: Project) : DialogWrapper(project) {

    private val jsonEditor = JsonEditorTextPanel(project, testJson)

    init {
        super.init()
        title = "Json to Dart Macro"
        setSize(500, 500)
    }

    override fun createCenterPanel(): JComponent {
        return jsonEditor
    }

    override fun doValidate(): ValidationInfo? {
        if (jsonEditor.text.trim()
                .isBlank()
        ) return ValidationInfoBuilder(jsonEditor).error(PluginBundle.get("input.your.json"))
        if (!jsonEditor.text.validParseToFreezed()) return ValidationInfoBuilder(jsonEditor).error(PluginBundle.get("json.format.verification.failed"))
        return super.doValidate()
    }

    override fun doOKAction() {
        super.doOKAction()
        DartMacroDialog(project, jsonEditor.text).show()
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(500, 500)
    }

    override fun getSize(): Dimension {
        return Dimension(500, 500)
    }

}


///生成窗口
class DartMacroDialog(val project: Project, json: String) : DialogWrapper(project) {

    private val objs: List<MyChildObject> = MyJsonParseTool.parseJson(json).filterIsInstance<MyChildObject>()

    private val tab = JBTabbedPane()
    private val classConfig = DartMarcoClassConfig()

    init {
        super.init()
        title = "Flutterx Json to Dart Macro Code Generate"
        setSize(500, 500)
        objs.forEach {
            tab.add(it.className, ClassCodeEditor(project, it))
        }
    }

    override fun createCenterPanel(): JComponent {
        return BorderLayoutPanel().addToCenter(tab).addToBottom(createSettingPanel())
    }

    private fun createSettingPanel(): DialogPanel {
        return panel {
            row {
                checkBox(PluginBundle.get("freezed.gen.base.open.in.editor")).bindSelected(classConfig::openInEditor)
            }
        }
    }


}


///代码编辑区域
private class ClassCodeEditor(project: Project, val obj: MyChildObject) :
    BorderLayoutPanel() {
    val dartEdit = DartEditorTextPanel(project = project, obj.generateDartMacro())

    private val settingPanel = panel {
        row("class name: ") {
            textField().bindText(obj::className)
        }
    }

    init {
        addToCenter(dartEdit)
        addToBottom(settingPanel)
    }
}


class DartMacroAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        EnterJsonToDartMacroDialog(e.project!!).show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        e.presentation.icon = AllIcons.General.Beta
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}