package shop.itbug.fluttercheckversionx.dialog

import cn.hutool.core.swing.ScreenUtil
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.common.getVirtualFile
import shop.itbug.fluttercheckversionx.config.JsonToFreezedSettingModelConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.services.DEFAULT_CLASS_NAME
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.FreezedCovertModelWidget
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.SwingUtilities


///json转freezed弹窗
class FreezedClassesGenerateDialog(
    val project: Project, private val freezedClasses: MutableList<FreezedCovertModel>
) : DialogWrapper(project) {
    private val settingInstance = JsonToFreezedSettingModelConfig.getInstance(project)
    private val setting = settingInstance.state
    private val tabView = JBTabbedPane().apply {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
    }
    private var fileName: String = DEFAULT_CLASS_NAME
    private var filePath: String = setting.generateToPath
    private val widgets: MutableList<FreezedCovertModelWidget> = mutableListOf()
    private lateinit var settingPanel: DialogPanel
    private lateinit var nameLabel: Cell<JBTextField>

    init {
        super.init()
        initTabView()
        title = PluginBundle.get("freezed.title")
        setOKButtonText(PluginBundle.get("freezed.btn.ok"))
        setCancelButtonText(PluginBundle.get("cancel"))
    }


    private fun initTabView() {
        freezedClasses.forEach {
            val widget = FreezedCovertModelWidget(it, project, disposable)
            widgets.add(widget)
            tabView.add(it.className, widget)
        }
    }

    override fun createCenterPanel(): JComponent {
        return object : BorderLayoutPanel() {
            init {
                addToCenter(tabView)
                addToBottom(getGlobalSettingPanel())
            }
        }
    }


    /**
     * 存储设置
     */
    fun getGlobalSettingPanel(): DialogPanel {

        val alarm = Alarm(disposable)

        fun doValid() {
            alarm.addRequest({
                settingPanel.apply()
                settingPanel.validate()
                doValid()
            }, 1000)
        }

        settingPanel = panel {
            group(PluginBundle.get("global.settings")) {
                row(PluginBundle.get("save.to.directory")) {
                    textFieldWithBrowseButton(
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .withRoots(project.guessProjectDir()), project = project
                    ).bindText({ filePath }, { filePath = it }).align(Align.FILL)
                }
                row(PluginBundle.get("file.name")) {
                    nameLabel = textField().bindText({ fileName }, {
                        fileName = it
                    }).align(Align.FILL)
                        .cellValidation {
                        }
                }
                row {
                    checkBox("${PluginBundle.get("automatic.operation.command")} flutter pub run build_runner build").bindSelected(
                        { setting.autoRunDartBuilder },
                        { v ->
                            settingInstance.changeState { it.copy(autoRunDartBuilder = v) }
                        }).align(Align.FILL)
                }
            }
        }


        //如果路径为空,设置为项目目录
        SwingUtilities.invokeLater {
            settingPanel.registerValidators(disposable)
            doValid()
            if (filePath.isBlank()) {
                filePath = project.guessProjectDir()?.path ?: ""
                settingPanel.apply()
            }
        }
        return settingPanel.apply { border = null }
    }


    private fun getPsiFile(): PsiFile {
        settingPanel.apply()
        settingInstance.changeState { it.copy(generateToPath = filePath) }
        val psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText("$fileName.dart", DartLanguage.INSTANCE, generateFileText())
        println(psiFile.virtualFile.exists())
        return psiFile
    }

    override fun doOKAction() {
        val psiFile = getPsiFile()
        val virtualFile = filePath.getVirtualFile()
        if (virtualFile == null) {
            project.toastWithError(PluginBundle.get("unable.to.find.directory"))
        } else {
            val findDirectory = PsiManager.getInstance(project).findDirectory(virtualFile)
            if (findDirectory == null) {
                project.toastWithError(PluginBundle.get("unable.to.find.directory"))
            }

            var isSuccess = true
            findDirectory?.let { pd ->
                runWriteAction {
                    try {
                        pd.add(psiFile)
                    } catch (e: Exception) {
                        isSuccess = false
                        project.toastWithError("error:$e")
                    }
                }
                if (isSuccess) {
                    FileEditorManager.getInstance(project).openFile(psiFile.virtualFile)
                    project.toast(PluginBundle.get("build.succeeded"))
                    if (setting.autoRunDartBuilder) {
                        RunUtil.runCommand(project, "freezed gen", "flutter pub run build_runner build")
                    }
                    FileBasedIndex.getInstance().requestReindex(virtualFile)
                    super.doOKAction()
                }
            }
        }

    }

    override fun doCancelAction() {
        settingPanel.apply()
        ///保存路径
        settingInstance.changeState { it.copy(generateToPath = filePath) }
        super.doCancelAction()
    }

    private fun generateFileText(): String {
        val sb = StringBuilder()
        sb.appendLine("import 'package:freezed_annotation/freezed_annotation.dart';")
        sb.appendLine()
        sb.appendLine("part '$fileName.freezed.dart';")
        sb.appendLine("part '$fileName.g.dart';")
        sb.appendLine()
        widgets.forEach {
            sb.appendLine()
            sb.appendLine(it.code)
            sb.appendLine()
        }
        return sb.toString()
    }


    override fun getPreferredSize(): Dimension {
        return ScreenUtil.dimension
    }
}
