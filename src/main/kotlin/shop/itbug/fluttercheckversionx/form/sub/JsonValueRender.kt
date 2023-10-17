package shop.itbug.fluttercheckversionx.form.sub

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel

/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */

class JsonValueRender(p: Project) : JsonEditorTextPanel(p) {


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?) {
        if (json != null && !project.isDisposed) {
            val changeJson = changeJson(json)
            WriteCommandAction.runWriteCommandAction(project) {
                text = changeJson
            }
            super.scrollToTop()
        }
    }

    /**
     * 改变显示内容
     *
     * 返回要显示的json string
     */
    private fun changeJson(json: Any): String {
        val isJson = if (json is String) JSON.isValid(json) else false
        return if (isJson) {
            return JSON.toJSONString(
                JSON.parseObject(json.toString(), Map::class.java),
                JSONWriter.Feature.PrettyFormat
            )
        } else {
            try {
                JSON.toJSONString(json, JSONWriter.Feature.PrettyFormat)
            } catch (_: Exception) {
                json.toString()
            }
        }
    }

}

//private fun <T : JComponent> T.createAnActions(text: String): Array<AnAction> {
//    return arrayOf(
//        MyActionButton(JsonToFreezedModelAction(text)).action,
//        WidgetUtil.getCopyAnAction(text),
//        WidgetUtil.getDiscordAction()
//    )
//}


