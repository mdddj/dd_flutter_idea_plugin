package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.util.toastWithError
import javax.swing.AbstractListModel
import javax.swing.ListCellRenderer
import javax.swing.SwingUtilities

//城市列表选择组件
class JobsCitySelectWidget(val project: Project) : JBList<ResourceCategory>() {


    class SimpleListModel(val list: List<ResourceCategory>) : AbstractListModel<ResourceCategory>() {
        override fun getSize(): Int = list.size
        override fun getElementAt(p0: Int): ResourceCategory = list[p0]
    }

    init {
        SwingUtilities.invokeLater {
            refreshCityList()
        }

        cellRenderer =
            ListCellRenderer { _, p1, _, _, _ -> JBLabel(p1.name) }

        emptyText.apply {
            appendLine("暂无城市列表.")
            appendLine("刷新", SimpleTextAttributes(SimpleTextAttributes.STYLE_HOVERED,JBUI.CurrentTheme.Link.Foreground.ENABLED)){
                refreshCityList()
            }
        }

    }

    private fun refreshCityList() {
        SERVICE.create<ItbugService>().findAllJobCity()
            .enqueue(object : Callback<JSONResult<List<ResourceCategory>>> {
                override fun onResponse(
                    call: Call<JSONResult<List<ResourceCategory>>>,
                    response: Response<JSONResult<List<ResourceCategory>>>
                ) {
                    response.body()?.apply {
                        if (this.state == 200) {
                            model = SimpleListModel(this.data)
                        }
                    }
                }

                override fun onFailure(call: Call<JSONResult<List<ResourceCategory>>>, t: Throwable) {
                    project.toastWithError("加载城市列表失败:$t")
                }

            })
    }

}