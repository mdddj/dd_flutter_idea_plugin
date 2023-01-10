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
        title = "生成资产文件配置"
        isResizable = false

    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("类名") {
                textField()
            }
            row ("文件名") {
                textField()
            }
            row {
                checkBox("设置为默认且不再弹窗提醒")
            }.bottomGap(BottomGap.SMALL)
            row {
                button("生成"){

                }.align(Align.FILL)
            }
        }
    }

    override fun createActions(): Array<Action> {
        return emptyArray()
    }

}

