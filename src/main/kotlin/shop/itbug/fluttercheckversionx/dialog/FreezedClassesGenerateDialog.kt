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
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.common.getVirtualFile
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
//    project.basePath + "/lib/freezed"
    private var filePath: String =  ""
    private val widgets : MutableList<FreezedCovertModelWidget> = mutableListOf()
    private lateinit var settingPanel: DialogPanel
    init {
        super.init()
        title = "freezed类生成"
        initTabView()
        setOKButtonText("一键生成")
        setCancelButtonText("取消")
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
            row("保存到目录") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                        roots = ProjectRootManager.getInstance(project).contentRoots.toMutableList()
                    },
                    project = project
                ).align(Align.FILL).bindText({
                   filePath
                }, {
                   filePath = it
                })

            }.contextHelp("如果目录不存在,将会自动穿件该文件夹", "提示")
            row("文件名") {
                textField().bindText({ fileName }, {
                    fileName = it
                })
            }
            row {
                checkBox("创建完自动运行flutter pub run build_runner build 命令").bindSelected({true},{

                })
            }
        }
        return settingPanel
    }


    override fun doOKAction() {
        settingPanel.apply()
        val psiFile =
            PsiFileFactory.getInstance(project).createFileFromText("$fileName.dart",DartLanguage.INSTANCE, generateFileText())
        val virtualFile = filePath.getVirtualFile()
        if(virtualFile==null){
            project.toastWithError("无法找到目录")
        }
        virtualFile?.let {
            val  findDirectory = PsiManager.getInstance(project).findDirectory(it)
            if(findDirectory==null){
                project.toastWithError("查找目录失败")
            }
            findDirectory?.let {
                runWriteAction {
                    findDirectory.add(psiFile)
                }
                project.toast("创建成功")
                super.doOKAction()
            }

        }


    }

    private fun generateFileText() : String {
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