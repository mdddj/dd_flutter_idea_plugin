package shop.itbug.fluttercheckversionx.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import groovy.json.JsonException
import kotlinx.serialization.json.Json
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
        if (!isValidJson(text)) {
            event.project?.toastWithError(PluginBundle.get("json.format.verification.failed"))
            return
        }
        event.project?.jsonToFreezedRun(text)
    }
}

fun isValidJson(jsonString: String): Boolean {
    return try {
        Json.parseToJsonElement(jsonString)
        true
    } catch (_: JsonException) {
        false
    }
}