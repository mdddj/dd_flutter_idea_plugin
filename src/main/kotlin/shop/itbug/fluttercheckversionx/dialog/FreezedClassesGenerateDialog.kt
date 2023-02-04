package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.lang.dart.DartLanguage
import org.jetbrains.plugins.terminal.TerminalView
import shop.itbug.fluttercheckversionx.common.getVirtualFile
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.services.DEFAULT_CLASS_NAME
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.FreezedCovertModelWidget
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel


class FreezedClassesGenerateDialog(val project: Project, private val freezedClasses: MutableList<FreezedCovertModel>) :
    DialogWrapper(project) {

    private val tabView = JBTabbedPane()
    private var fileName: String = DEFAULT_CLASS_NAME
    private var filePath: String = ""
    private val widgets: MutableList<FreezedCovertModelWidget> = mutableListOf()
    private lateinit var settingPanel: DialogPanel
    private var autoRunBuildCommod: Boolean = true

    init {
        super.init()
        title = PluginBundle.get("freezed.title")
        initTabView()
        setOKButtonText(PluginBundle.get("freezed.btn.ok"))
        setCancelButtonText(PluginBundle.get("cancel"))
    }


    private fun initTabView() {
        freezedClasses.forEach {
            val widget = FreezedCovertModelWidget(it, project)
            widgets.add(widget)
            tabView.add(it.className, widget)
        }
    }

    override fun createCenterPanel(): JComponent {
        return object : JPanel(BorderLayout()) {

            init {
                add(tabView, BorderLayout.CENTER)
                add(getGlobalSettingPanel(), BorderLayout.SOUTH)
            }
        }
    }


    /**
     * 存储设置
     */
    fun getGlobalSettingPanel(): DialogPanel {
        settingPanel = panel {
            group(PluginBundle.get("global.settings")) {
                row(PluginBundle.get("save.to.directory")) {
                    textFieldWithBrowseButton(
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                            roots = ProjectRootManager.getInstance(project).contentRoots.toMutableList()
                        },
                        project = project
                    ).bindText({
                        filePath
                    }, {
                        filePath = it
                    })

                }
                row(PluginBundle.get("file.name")) {
                    textField().bindText({ fileName }, {
                        fileName = it
                    })
                }
                row {
                    checkBox("${PluginBundle.get("automatic.operation.command")} flutter pub run build_runner build").bindSelected({ autoRunBuildCommod },
                        {
                            autoRunBuildCommod = it
                        })
                }
            }
        }
        return settingPanel
    }


    override fun doOKAction() {
        settingPanel.apply()
        val psiFile =
            PsiFileFactory.getInstance(project)
                .createFileFromText("$fileName.dart", DartLanguage.INSTANCE, generateFileText())
        val virtualFile = filePath.getVirtualFile()
        if (virtualFile == null) {
            project.toastWithError(PluginBundle.get("unable.to.find.directory"))
        }else{
            val findDirectory = PsiManager.getInstance(project).findDirectory(virtualFile)
            if (findDirectory == null) {
                project.toastWithError(PluginBundle.get("unable.to.find.directory"))
            }
            findDirectory?.let {
                runWriteAction {
                    findDirectory.add(psiFile)
                }
                project.toast(PluginBundle.get("build.succeeded"))
                if (autoRunBuildCommod) {
                    TerminalView.getInstance(project).createLocalShellWidget(project.basePath, "freezed gen").executeCommand("flutter pub run build_runner build")
                }
                FileBasedIndex.getInstance().requestReindex(virtualFile)
                super.doOKAction()
            }
        }
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
}