package shop.itbug.fluttercheckversionx.actions

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyAction
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard

class ApiCopyAll:MyAction({"Copy All"}) {
    override fun actionPerformed(e: AnActionEvent) {
        val api = e.api()!!
        val dataMap = mapOf(
            "url" to api.url,
            "method" to api.method,
            "headers" to api.headers,
            "queryParams" to api.queryParams,
            "bodyJsonObject" to api.body,
            "statusCode" to api.statusCode,
            "body" to api.body,
            "requestTime" to api.createDate,
            "timestamp" to api.timestamp
        )
        val toJSONString = JSON.toJSONString(dataMap, JSONWriter.Feature.PrettyFormat)
        toJSONString.copyTextToClipboard()
    }


    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.api()!=null
        super.update(e)
    }
}