package shop.itbug.fluttercheckversionx.dialog.macro

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.Alarm
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.dialog.*
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.tools.*
import shop.itbug.fluttercheckversionx.util.FileWriteService
import shop.itbug.fluttercheckversionx.util.Listener
import shop.itbug.fluttercheckversionx.util.MyAlarm
import shop.itbug.fluttercheckversionx.widget.DartEditorTextPanel
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import javax.swing.JComponent
import javax.swing.SwingUtilities

private val testJson = """
    {
      "hello_world": "你好世界!"
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


}


///生成dart marco dialog
class DartMacroDialog(val project: Project, json: String) : DialogWrapper(project) {

    private val objs: List<MyChildObject> = MyJsonParseTool.parseJson(json).filterIsInstance<MyChildObject>()
    private val tab = JBTabbedPane()
    private lateinit var myPanel: DialogPanel
    private val classConfig: DartMarcoClassConfig = DartMarcoClassConfigStateService.getInstance(project).state
    private val editList: List<ClassCodeEditor> = objs.map { ClassCodeEditor(project, it, disposable, classConfig) }
    private lateinit var myAlarm: MyAlarm

    init {
        super.init()
        title = "Flutterx Json to Dart Macro Code Generate"
        setSize(500, 500)
        editList.forEach {
            tab.add(it.obj.className, it)
        }
    }

    override fun createCenterPanel(): JComponent {
        return BorderLayoutPanel().addToCenter(tab).addToBottom(createSettingPanel())
    }

    ///监听class 全局变化
    private val listen: Listener = {
        if (it) {
            myPanel.apply()
            editList.forEach { edit -> edit.changeClassConfig(classConfig) }
            DartMarcoClassConfigStateService.getInstance(project).loadState(classConfig)
        }
    }

    private fun createSettingPanel(): DialogPanel {
        myPanel = panel {
            row {
                saveToDirectoryConfig(
                    project, SaveToDirectoryModelOnChange(
                        onFilenameChange = classConfig::filename,
                        onDirectoryChange = classConfig::saveDir,
                        onOpenInEditor = classConfig::openInEditor
                    )
                ).align(Align.FILL)
            }
            nameRuleConfig(
                onChange = NameRuleConfig(
                    className = classConfig::classNameRule, propertiesName = classConfig::propertiesNameRule
                )
            )
        }
        myAlarm = MyAlarm(disposable, myPanel)

        SwingUtilities.invokeLater {
            myAlarm.start(listen)
            myPanel.registerValidators(disposable)
        }
        return myPanel
    }


    override fun doOKAction() {
        writeFile {
            super.doOKAction()
        }
    }


    private fun writeFile(onSuccess: () -> Unit) {
        val sb = StringBuilder()
        editList.forEach {
            val text = it.getClassText()
            sb.appendLine(text)
            sb.appendLine()
        }
        FileWriteService.getInstance(project).writeTo(
            sb.toString(), classConfig.filename, classConfig.saveDir
        ) { _, file ->
            run {
                onSuccess.invoke()
                if (classConfig.openInEditor) {
                    FileEditorManager.getInstance(project).openFile(file)
                }
            }
        }
    }
}


///代码编辑区域
private class ClassCodeEditor(
    project: Project,
    val obj: MyChildObject,
    disposable: Disposable,
    initConfig: DartMarcoClassConfig
) : BorderLayoutPanel() {
    private val dartEdit = DartEditorTextPanel(project = project, obj.generateDartMacro(initConfig))
    private val alarm = Alarm(disposable)
    private val settingPanel = panel {
        row("class name: ") {
            textField().bindText(obj::className)
        }
    }

    init {
        addToCenter(dartEdit)
        addToBottom(settingPanel)
        SwingUtilities.invokeLater {
            listenChange()
        }
    }

    /**
     * 当class 生成规则被改变
     */

    fun changeClassConfig(newConfig: DartMarcoClassConfig) {
        val newClassText = obj.generateDartMacro(newConfig)
        dartEdit.text = newClassText
    }

    fun getClassText(): String {
        return dartEdit.text
    }

    private fun listenChange() {
        alarm.addRequest({
            val isModified = settingPanel.isModified()
            if (isModified) {
                settingPanel.apply()
                dartEdit.text = obj.generateDartMacro()
            }
            listenChange()
        }, 1000)
    }
}


class DartMacroAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        EnterJsonToDartMacroDialog(e.project!!).show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}