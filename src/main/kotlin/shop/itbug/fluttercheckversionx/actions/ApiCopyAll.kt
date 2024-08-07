package shop.itbug.fluttercheckversionx.actions

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard

class ApiCopyAll : MyAction({ "Copy All" }) {
    override fun actionPerformed(e: AnActionEvent) {


        val api = e.api()!!
        val dataMap = mutableMapOf(
            "url" to api.url,
            "method" to api.method,
            "headers" to api.headers,
        )

        api.queryParams?.apply {
            if (this.isNotEmpty()) {
                dataMap["queryParams"] = this
            }
        }
        api.body?.apply {
            if (this is Map<*, *> && this.isNotEmpty()) {
                dataMap["body"] = this
            }
        }

        dataMap["responseStatusCode"] = api.statusCode
        dataMap["response"] = api.getDataJson()
        dataMap["requestTime"] = api.createDate
        dataMap["timestamp"] = api.timestamp

        val string = Gson().toJson(dataMap)
        string.copyTextToClipboard()
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.api() != null
        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}