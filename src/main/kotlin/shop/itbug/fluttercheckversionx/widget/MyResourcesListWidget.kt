package shop.itbug.fluttercheckversionx.widget

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.model.Pageable
import shop.itbug.fluttercheckversionx.model.resource.MyResource
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.style.RoundBorderStyle
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import java.awt.Component
import javax.swing.AbstractListModel
import javax.swing.JList
import javax.swing.ListCellRenderer

class MyResourcesListWidget(val project: Project) : JBList<MyResource>() {


    data class MyResourcesListWidgetModel(val list: List<MyResource>) : AbstractListModel<MyResource>() {
        override fun getSize(): Int = list.size
        override fun getElementAt(index: Int): MyResource = list[index]

    }

    class MyResourcesListWidgetRender : ListCellRenderer<MyResource> {
        override fun getListCellRendererComponent(
            list: JList<out MyResource>?,
            value: MyResource?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            value?.let {
                return panel {
                    row {
                        label(it.title).component.apply {
                            font = JBFont.h1()
                        }
                    }
                    row {
                        label(it.user.nickName).component.apply {
                            font = JBFont.label()
                        }
                        label(it.createDate).component.apply {
                            font = JBFont.label()
                        }
                        label(it.category.name).component.apply {
                            font =  JBFont.label()
                        }
                        label(it.label).component.apply {
                            font = JBFont.label()
                            foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
                        }
                        cell(JBLabel(AllIcons.Actions.DeleteTag)).align(Align.FILL)
                    }
                }.apply {
                    border = RoundBorderStyle()
                }
            }
            return JBLabel()
        }
    }

    init {
        cellRenderer = MyResourcesListWidgetRender()
    }

    fun getJobsWithId(id: Long) {
        SERVICE.create<ItbugService>().findAllJob(mutableMapOf("cateId" to id, "page" to 0, "pageSize" to 10))
            .enqueue(object :
                Callback<JSONResult<Pageable<MyResource>>> {
                override fun onResponse(
                    call: Call<JSONResult<Pageable<MyResource>>>,
                    response: Response<JSONResult<Pageable<MyResource>>>
                ) {
                    project.toast("获取列表成功")
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            if (body.data.content.isNotEmpty()) {
                                model = MyResourcesListWidgetModel(body.data.content)
                            }
                        }

                    }
                }

                override fun onFailure(call: Call<JSONResult<Pageable<MyResource>>>, t: Throwable) {
                    project.toastWithError("${t.message}")
                }

            })
    }

}