package shop.itbug.fluttercheckversionx.form.actions

import java.awt.Component
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer


/**
 * 筛选状态码
 */
class StateCodeFilterBox : JComboBox<String>() {


    init {



    }



}


/**
 * item渲染
 */
class CellRenderModel : ListCellRenderer<String> {

    override fun getListCellRendererComponent(
        list: JList<out String>?,
        value: String?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {

        return JLabel(value)
    }

}