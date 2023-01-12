package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import javax.swing.Action
import javax.swing.JComponent

//生成资产文件的配置弹窗
class AssetsAutoGenerateClassActionConfigDialog(project: Project) : DialogWrapper(project) {

    init {
        super.init()
        title = "生成资产文件类"
        isResizable = false

    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("类    名") {
                textField()
            }
            row ("文件名") {
                textField()
            }
            row("目    录") {
                textField()
            }
            row {
                checkBox("保存且不再提醒")
            }.contextHelp("可在设置中再次配置")
            row {
                checkBox("监听文件变化自动更新")
            }.bottomGap(BottomGap.SMALL)
            row {
                button("生成"){

                }.align(Align.FILL)
            }
            row {
                comment("有任何问题请<a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues'>提交bug</a>")
            }
        }
    }

    override fun createActions(): Array<Action> {
        return emptyArray()
    }

}

