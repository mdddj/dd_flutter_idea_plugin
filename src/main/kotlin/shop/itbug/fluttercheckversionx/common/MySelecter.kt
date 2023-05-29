package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.components.JBList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.services.JSONResult
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListCellRenderer




abstract class MySelecterAction : MyComboBoxAction() {
    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        
        return super.createPopupActionGroup(button, dataContext)
    }
}

///我的选择器
abstract class MySelecter<T> : JBList<T>() {


    val listModel = DefaultListModel<T>()
    private val listRender = ListCellRenderer { _, value, index, isSelected, _ -> getCompeont(value, index,isSelected) }
    abstract fun getData(): Call<JSONResult<List<T>>>


    init {
        model = listModel
        cellRenderer = listRender
        this.getData().enqueue(object : Callback<JSONResult<List<T>>> {
            override fun onResponse(call: Call<JSONResult<List<T>>>, response: Response<JSONResult<List<T>>>) {
                response.body()?.data?.apply {
                    listModel.addAll(this)
                }
            }

            override fun onFailure(call: Call<JSONResult<List<T>>>, throwable: Throwable) {
                println("加载失败")
            }

        })
    }

    
    abstract fun getCompeont(value: T, index: Int, isSelected : Boolean) : JComponent

}


