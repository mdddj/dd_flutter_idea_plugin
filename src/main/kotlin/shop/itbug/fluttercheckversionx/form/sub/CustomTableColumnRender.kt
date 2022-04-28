package shop.itbug.fluttercheckversionx.form.sub

import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.event.MouseInputListener
import javax.swing.table.DefaultTableCellRenderer

///自定义表格渲染内容
class CustomTableColumnRender : DefaultTableCellRenderer(), MouseInputListener {

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {

        ///状态码的列
        if (column == 2) {
            val statusCode = value.toString()
            val label = JLabel(statusCode)
            label.foreground = if (statusCode != "200") {
                Color.RED
            } else {
                Color.GREEN
            }
            return label
        }

        ///URL的列
        if (column == 0) {
            val label = JLabel(value.toString())
            label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            label.text = "<html><u>" + value.toString() + "</u></html>"
            return label
        }



        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    }

    override fun mouseClicked(e: MouseEvent?) {
    }

    override fun mousePressed(e: MouseEvent?) {
    }

    override fun mouseReleased(e: MouseEvent?) {
    }

    override fun mouseEntered(e: MouseEvent?) {
    }

    override fun mouseExited(e: MouseEvent?) {
    }

    override fun mouseDragged(e: MouseEvent?) {
    }

    override fun mouseMoved(e: MouseEvent?) {
        TODO("Not yet implemented")
    }
}