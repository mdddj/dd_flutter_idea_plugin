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
import shop.itbug.fluttercheckversionx.util.MyActionUtil
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.util.toolbar
import java.awt.BorderLayout


class CityListView(val project: Project) : JBPanel<CityListView>(BorderLayout()) {


    private val toolbar = MyActionUtil.jobCityToolbarActionGroup.toolbar("城市列表")

    init {
        add(toolbar.component, BorderLayout.NORTH)
        add(JobsCitySelectWidget(project), BorderLayout.CENTER)
    }

    private fun addNewCity(name: String) {
        SERVICE.create<ItbugService>().addNewJobsCity(AddCityApiModel(name = name))
            .enqueue(object : Callback<JSONResult<Any>> {
                override fun onResponse(call: Call<JSONResult<Any>>, response: Response<JSONResult<Any>>) {
                    project.toast(response.body()?.message ?: response.message())
                }

                override fun onFailure(call: Call<JSONResult<Any>>, t: Throwable) {
                    project.toastWithError("添加失败")
                }
            })
    }


}