package shop.itbug.fluttercheckversionx.dialog.freezed

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.Alarm
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.dialog.*
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.tools.*
import shop.itbug.fluttercheckversionx.util.FileWriteService
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.VerifyFileDir
import shop.itbug.fluttercheckversionx.widget.DartEditorTextPanel
import javax.swing.JComponent
import javax.swing.SwingUtilities


///json转freezed对象的弹出
class StringToFreezedDialog(val project: Project, jsonString: String) : DialogWrapper(project, true) {
    private val objects: List<MyChildObject> = MyJsonParseTool.parseJson(jsonString).filterIsInstance<MyChildObject>()
        .mapIndexed { index, t -> t.copy(index = index) }
    private val tabs = JBTabbedPane()
    private val generateConfig = FreezedClassConfigStateService.getInstance(project).state
    private val panels =
        objects.map { RustEditorPanel(project, it, disposable, generateConfig.copy(objectIndex = objects.indexOf(it))) }
    private val alarm = Alarm(disposable)
    private lateinit var settingPanel: DialogPanel
    private lateinit var dirField: Cell<TextFieldWithBrowseButton>

    init {
        super.init()
        title = "FlutterX Freezed Code Generate Tool"
        panels.forEach {
            tabs.add(it.dartClass.className, it)
        }
        SwingUtilities.invokeLater {
            listenChange()
        }
    }

    private fun listenChange() {
        alarm.addRequest({
            if (settingPanel.isModified()) {
                settingPanel.apply()
                FreezedClassConfigStateService.getInstance(project).loadState(generateConfig)
                panels.forEach { it.changeText(generateConfig.copy(objectIndex = panels.indexOf(it))) }
            }
            listenChange()
        }, 1000)
    }

    override fun createCenterPanel(): JComponent {
        settingPanel = panel {
            row {
                panel {
                    group(PluginBundle.get("freezed.gen.group.title") + ":") {
                        row {
                            checkBox(PluginBundle.get("freezed.gen.group.add.structure.fun")).bindSelected(
                                generateConfig::addStructureFunction
                            )
                        }
                        row {
                            checkBox(PluginBundle.get("freezed.gen.group.add.fromjson.fun") + "(FromJson)").bindSelected(
                                generateConfig::addFromJsonFunction
                            )
                        }
                        row {
                            checkBox(PluginBundle.get("freezed.gen.base.set.default.value")).bindSelected(generateConfig.propsConfig::setDefaultValue)
                        }
                        val box = ComboBox(FormJsonType.entries.toTypedArray())
                        row("fromJson ${PluginBundle.get("freezed.gen.formatname.fromjson.type")}") {
                            cell(box).bindItem(generateConfig::formJsonType)
                        }
                        // freezed新版本设置
                        FreezedNewSetting.setting(generateConfig, this,project)
                    }
                }.gap(RightGap.COLUMNS).align(AlignY.TOP).resizableColumn()


                ///保存到目录
                saveToDirectoryConfig(
                    project, SaveToDirectoryModelOnChange(
                        { generateConfig.saveDirectory },
                        { generateConfig.saveDirectory = it },
                        generateConfig::saveFileName,
                        generateConfig::openInEditor
                    )
                ) {
                    row {
                        checkBox("${PluginBundle.get("automatic.operation.command")} flutter pub run build_runner build").bindSelected(
                            generateConfig::runBuildCommand
                        )
                    }
                }.align(AlignY.TOP).resizableColumn()
            }

            ///命名规则
            nameRuleConfig(
                NameRuleConfig(
                    classNameNew = generateConfig::classNameFormatNew,
                    propertyNameNew = generateConfig::propertyNameFormatNew,
                    classNameRaw = generateConfig::classNameRaw,
                    propertyNameRaw = generateConfig::propertyNameRaw,
                )
            )



            collapsibleGroup("<html>Hive & Isar ${PluginBundle.get("freezed.gen.base.setting")}</html>", false) {
                row {
                    panel {
                        group("Hive") {
                            row {
                                checkBox(PluginBundle.get("freezed.gen.create.enable")).bindSelected(generateConfig.hiveSetting::enable)
                                intTextField().bindIntText(generateConfig.hiveSetting::hiveId).label("HiveType id")
                            }
                        }
                    }.gap(RightGap.COLUMNS).align(AlignY.TOP).resizableColumn()
                    panel {
                        group("Isar") {
                            row {
                                checkBox("Use isar").bindSelected(generateConfig::useIsar)
                            }
                        }
                    }.align(AlignY.TOP).resizableColumn()
                }
            }
        }
        settingPanel.registerValidators(disposable)
        return BorderLayoutPanel().addToCenter(tabs).addToBottom(settingPanel)
    }

    override fun doValidate(): ValidationInfo? {
        val d = VerifyFileDir.validDirByPath(generateConfig.saveDirectory)
        if (d) {
            return ValidationInfoBuilder(dirField.component).error(VerifyFileDir.ERROR_MSG)
        }
        return super.doValidate()
    }

    override fun doOKAction() {
        settingPanel.apply()
        doCreateFile { super.doOKAction() }
    }

    ///创建文件并添加
    private fun doCreateFile(onSuccess: () -> Unit) {
        val sb = StringBuilder()
        val fileName = generateConfig.saveFileName
        val dirPath = generateConfig.saveDirectory
        sb.appendLine("import 'package:freezed_annotation/freezed_annotation.dart';")
        sb.appendLine("part '$fileName.freezed.dart';")
        if (generateConfig.addFromJsonFunction) {
            sb.appendLine("part '$fileName.g.dart';")
        }
        panels.forEach {
            sb.appendLine("")
            sb.appendLine(it.getObjectClassText())
            sb.appendLine()
        }

        FileWriteService.getInstance(project).writeTo(sb.toString(), fileName, dirPath) { _, psiFile ->
            run {
                if (generateConfig.openInEditor) {
                    FileEditorManager.getInstance(project).openFile(psiFile)
                }
                if (generateConfig.runBuildCommand) {
                    RunUtil.runFlutterBuildCommand(project)
                }
                onSuccess.invoke()
            }
        }
    }

}


private class RustEditorPanel(
    project: Project,
    val dartClass: MyChildObject,
    val parentDispose: Disposable,
    initConfig: FreezedClassConfig = FreezedClassConfig()
) : BorderLayoutPanel() {
    private val rustEditor = DartEditorTextPanel(project, dartClass.getFreezedClass(initConfig), false)
    private val newDispose = Disposer.newDisposable().apply { Disposer.register(parentDispose, this) }
    private val alarm = Alarm(newDispose)
    var globalConfig = FreezedClassConfig()
    var config = FreezedPropertiesConfig()
    private val settingPanel = panel {
        row("Class Name") {
            textField().bindText(dartClass::className)
        }
    }

    init {
        addToCenter(rustEditor)
        addToBottom(settingPanel)
        SwingUtilities.invokeLater {
            listenChange()
        }
    }

    private fun listenChange() {
        alarm.addRequest({
            if (settingPanel.isModified()) {
                settingPanel.apply()
                globalConfig = globalConfig.copy(propsConfig = config)
                changeText(globalConfig)
            }
            listenChange()
        }, 700)
    }

    fun changeText(config: FreezedClassConfig) {
        globalConfig = config
        rustEditor.text = dartClass.getFreezedClass(config)
    }

    fun getObjectClassText() = rustEditor.text

}

