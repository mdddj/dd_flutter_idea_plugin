package shop.itbug.fluttercheckversionx.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.PubPackage
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JTable

/**
 * dart pub 包一览表
 */
class DartPackageTable(val project: Project) : DialogWrapper(project, true) {

    private val table = JBTable()

    init {
        super.init()
        title = "Dart Packages"
        DartPackageCheckService.setJBTableData(table, project)
        DartPackageCheckService.setColumnWidth(table)
        setNewRender()
    }

    private fun setNewRender() {
        table.columnModel.getColumn(2).cellRenderer = object : ColoredTableCellRenderer() {
            override fun customizeCellRenderer(p0: JTable, p1: Any?, p2: Boolean, p3: Boolean, p4: Int, p5: Int) {
                if (p1 is PubPackage) {
                    if (p1.hasNew()) {
                        icon = AllIcons.General.Information
                    }
                    append("${p1.second?.latest}")
                }


            }
        }
    }

    override fun createCenterPanel(): JComponent {
        return JBScrollPane(table).apply {
            minimumSize = Dimension(0, 600)
        }
    }


}