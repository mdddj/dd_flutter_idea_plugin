package shop.itbug.fluttercheckversionx.dialog.freezed

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.Alarm
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.DartFileType
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.tools.*
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.VerifyFileDir
import shop.itbug.fluttercheckversionx.widget.DartEditorTextPanel
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.io.path.Path


///json转freezed对象的弹出
class StringToFreezedDialog(val project: Project, jsonString: String) : DialogWrapper(project, true) {

    private val objects: List<MyChildObject> = MyJsonParseTool.parseJson(jsonString).filterIsInstance<MyChildObject>()
        .mapIndexed { index, t -> t.copy(index = index) }
    private val tabs = JBTabbedPane()
    private val panels = objects.map { RustEditorPanel(project, it, disposable) }
    private val generateConfig = FreezedClassConfigStateService.getInstance(project).state
    private val alarm = Alarm(disposable)
    private lateinit var settingPanel: DialogPanel
    private lateinit var filenameField: Cell<JBTextField>
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
            println(settingPanel.isModified())
            if (settingPanel.isModified()) {
                settingPanel.apply()
                FreezedClassConfigStateService.getInstance(project).loadState(generateConfig)
                panels.forEach { it.changeText(generateConfig) }
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
                    }
                }.gap(RightGap.COLUMNS).align(AlignY.TOP).resizableColumn()
                panel {
                    group(PluginBundle.get("save.to.directory")) {
                        row(PluginBundle.get("g.2")) {
                            filenameField = textField().align(Align.FILL).bindText(generateConfig::saveFileName)
                            filenameField.addValidationRule(VerifyFileDir.ENTER_YOU_FILE_NAME) {
                                it.text.trim().isBlank()
                            }
                            filenameField.validationOnInput {
                                println("text:${it.text}")
                                if (it.text.trim().isBlank()) {
                                    return@validationOnInput error(VerifyFileDir.ENTER_YOU_FILE_NAME)
                                }
                                return@validationOnInput null
                            }
                        }
                        row(PluginBundle.get("g.3")) {
                            dirField = textFieldWithBrowseButton(
                                "Select Dir", project, FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            ) { it.path }.bindText(generateConfig::saveDirectory).align(Align.FILL)
                                .addValidationRule(VerifyFileDir.ERROR_MSG) {
                                    VerifyFileDir.validDirByComponent(it)
                                }
                            dirField.validationOnInput {
                                if (VerifyFileDir.validDirByComponent(dirField.component)) {
                                    return@validationOnInput ValidationInfoBuilder(it.textField).error(VerifyFileDir.ERROR_MSG)
                                }
                                return@validationOnInput null
                            }
                        }
                        row {
                            checkBox("${PluginBundle.get("automatic.operation.command")} flutter pub run build_runner build").bindSelected(
                                generateConfig::runBuildCommand
                            )
                        }
                        row {
                            checkBox(PluginBundle.get("freezed.gen.base.open.in.editor")).bindSelected(generateConfig::openInEditor)
                        }
                    }
                }.align(AlignY.TOP).resizableColumn()
            }

            collapsibleGroup(PluginBundle.get("freezed.gen.base.opt")) {
                buttonsGroup(PluginBundle.get("freezed.gen.formatname.classname") + ":") {
                    row {
                        NameFormat.values().forEach {
                            radioButton(it.title, it)
                            contextHelp(it.example, PluginBundle.get("freezed.gen.formatname.example"))
                        }
                    }
                }.bind(generateConfig::classNameFormat)
                buttonsGroup(PluginBundle.get("freezed.gen.formatname.properties") + ":") {
                    row {
                        NameFormat.values().forEach {
                            radioButton(it.title, it)
                            contextHelp(it.example, PluginBundle.get("freezed.gen.formatname.example"))
                        }
                    }
                }.bind(generateConfig::propertyNameFormat)
                buttonsGroup("fromJson ${PluginBundle.get("freezed.gen.formatname.fromjson.type")}:") {
                    row {
                        FormJsonType.values().forEach {
                            radioButton(it.value, it)
                        }
                    }
                }.bind(generateConfig::formJsonType)
            }

            collapsibleGroup("Hive ${PluginBundle.get("freezed.gen.base.setting")}") {
                row {
                    checkBox(PluginBundle.get("freezed.gen.create.enable")).bindSelected(generateConfig.hiveSetting::enable)
                    intTextField().bindIntText(generateConfig.hiveSetting::hiveId).label("HiveType id")
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
        val dirVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(Path(dirPath))
        if (dirVirtualFile != null) {
            val findDirectory = PsiManager.getInstance(project).findDirectory(dirVirtualFile)
            if (findDirectory != null) {
                val createDartFile = PsiFileFactory.getInstance(project)
                    .createFileFromText("$fileName.dart", DartFileType.INSTANCE, sb.toString())
                try {
                    val psiElement = ApplicationManager.getApplication()
                        .runWriteAction(Computable { findDirectory.add(createDartFile) })
                    if (generateConfig.openInEditor) {
                        FileEditorManager.getInstance(project).openFile(psiElement.containingFile.virtualFile)
                    }
                    if (generateConfig.runBuildCommand) {
                        RunUtil.runFlutterBuildCommand(project)
                    }
                    onSuccess.invoke()
                } catch (e: Exception) {
                    showErrorMessage("${PluginBundle.get("freezed.gen.create.error")}:${e.localizedMessage}")
                }
            }
        }
    }

    private fun showErrorMessage(msg: String) {
        val groupId = "json_to_freezed_tooltip"
        NotificationGroupManager.getInstance().getNotificationGroup(groupId)
            .createNotification(msg, NotificationType.ERROR).notify(project)
    }
}


private class RustEditorPanel(val project: Project, val dartClass: MyChildObject, val parentDispose: Disposable) :
    BorderLayoutPanel() {
    private val rustEditor = DartEditorTextPanel(project, dartClass.getFreezedClass())
    private val newDispose = Disposer.newDisposable().apply { Disposer.register(parentDispose, this) }
    private val alarm = Alarm(newDispose)
    var globalConfig = FreezedClassConfig()
    var config = FreezedPropertiesConfig()
    private val settingPanel = panel {
        row("class Name") {
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
        }, 1000)
    }

    fun changeText(config: FreezedClassConfig) {
        globalConfig = config
        rustEditor.text = dartClass.getFreezedClass(config)
    }

    fun getObjectClassText() = rustEditor.text


}