package shop.itbug.fluttercheckversionx.form.actions

import java.awt.Component
import javax.swing.*


/**
 * 筛选状态码
 */
class StateCodeFilterBox(val filter: (type: String)->Unit) : JComboBox<String>()  {

    private  var methedTypes = listOf("All","Get","Post","Delete","Put")

    init {

        val renderModel = CellRender()
        setRenderer(renderModel)
        val myModel = CellModel(methedTypes = methedTypes)
        myModel.selectedItem = "All"
        model = myModel



        //添加选择监听器
        addActionListener { e ->
            e?.let {
                val item = model.selectedItem
                filter.invoke(item.toString())
            }
        }

    }

    override fun getItemCount(): Int {
        return methedTypes.size
    }






}




class CellModel (private var methedTypes: List<String>): DefaultComboBoxModel<String>() {

    override fun getSize(): Int {
        return methedTypes.size
    }


    override fun getElementAt(index: Int): String {
        return methedTypes[index]
    }




}

/**
 * item渲染
 */
class CellRender : ListCellRenderer<String> {




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