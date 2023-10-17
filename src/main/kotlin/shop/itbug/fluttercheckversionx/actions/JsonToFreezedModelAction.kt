package shop.itbug.fluttercheckversionx.actions

import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.toastWithError

class JsonToFreezedModelAction(private val text: String) : MyDumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        jsonToFreezedModel(e)
    }
    private fun jsonToFreezedModel(event: AnActionEvent) {
        val text = text.trim()
        if (text.isEmpty()) {
            event.project?.toastWithError(PluginBundle.get("input.your.json"))
            return
        }
        try {
            JSONObject.parseObject(text)
        } catch (e: Exception) {
            event.project?.toastWithError(PluginBundle.get("json.format.verification.failed"))
            return
        }
        event.project?.jsonToFreezedRun(text)
    }
}