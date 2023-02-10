package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.params.AddCityApiModel
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton


class CityListView(project: Project): JBPanel<CityListView>(BorderLayout()) {

    private val addButton: JButton = JButton("添加城市")

    init {
        maximumSize = Dimension(200,-1)
        preferredSize = Dimension(200,-1)
        add(addButton,BorderLayout.NORTH)
        addButton.addActionListener {
            SERVICE.create<ItbugService>().addNewJobsCity(AddCityApiModel(name = "广州")).enqueue(object : Callback<JSONResult<Any>>{
                override fun onResponse(call: Call<JSONResult<Any>>, response: Response<JSONResult<Any>>) {
                    project.toast(response.body()?.message ?: response.message())
                }

                override fun onFailure(call: Call<JSONResult<Any>>, t: Throwable) {
                    project.toastWithError("添加失败")
                }

            })
        }
    }


}