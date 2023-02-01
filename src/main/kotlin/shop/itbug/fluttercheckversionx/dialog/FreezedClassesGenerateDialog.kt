package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.services.DEFAULT_CLASS_NAME
import shop.itbug.fluttercheckversionx.widget.FreezedCovertModelWidget
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel


class FreezedClassesGenerateDialog(val project: Project, private val freezedClasses: MutableList<FreezedCovertModel>) :
    DialogWrapper(project) {

    private val tabView = JBTabbedPane()
    var fileName: String = DEFAULT_CLASS_NAME

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
        return panel {
            row("保存到目录") {
                textFieldWithBrowseButton(
                    fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    project = project
                ).align(Align.FILL).bindText({
                    project.basePath + "/lib/freezed"
                }, {
                    println("设置目录:${it}")
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
    }


    override fun doOKAction() {
        PsiFileFactory.getInstance(project).createFileFromText(DartLanguage.INSTANCE,"")
    }
}