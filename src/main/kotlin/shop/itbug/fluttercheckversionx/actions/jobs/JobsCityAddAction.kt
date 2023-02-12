package shop.itbug.fluttercheckversionx.actions.jobs

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.itbug.fluttercheckversionx.services.ItbugService
import shop.itbug.fluttercheckversionx.services.JSONResult
import shop.itbug.fluttercheckversionx.services.SERVICE
import shop.itbug.fluttercheckversionx.services.params.AddCityApiModel
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.WidgetUtil

/**
 * 添加城市操作
 */
class JobsCityAddAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {

        WidgetUtil.getTextEditorPopup("请输入城市名","例如:广州",{it.showInFocusCenter()}){
            addNewCity(it,e.project!!)
        }
    }

    /**
     * 请求api
     */
    private fun addNewCity(name: String,project: Project) {
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

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project!=null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}