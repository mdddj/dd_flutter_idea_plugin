package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.widget.FreezedCovertModelWidget
import javax.swing.JComponent


class FreezedClassesGenerateDialog(val project: Project, private val freezedClasses: MutableList<FreezedCovertModel>) : DialogWrapper(project) {

    private val tabView = JBTabbedPane()

    init {
        super.init()
        title = "freezed类生成"
        initTabView()
    }


    private fun initTabView() {
        freezedClasses.forEach {
            val widget = FreezedCovertModelWidget(it,project)
            tabView.add(it.className,widget)
        }
    }

    override fun createCenterPanel(): JComponent {

        return panel {
            row {
                scrollCell(tabView)
            }
        }
    }
}