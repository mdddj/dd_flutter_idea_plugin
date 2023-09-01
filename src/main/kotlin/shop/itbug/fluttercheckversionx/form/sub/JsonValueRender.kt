package shop.itbug.fluttercheckversionx.form.sub

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import javax.swing.BorderFactory

/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */

class JsonValueRender(var project: Project) : BorderLayoutPanel() {


    private var jsonView: LanguageTextField = JsonEditorTextPanel(project)

    val text: String get() = jsonView.text

    init {
        border = BorderFactory.createEmptyBorder()
        jsonView.border = BorderFactory.createEmptyBorder()
        jsonView.background = UIUtil.getPanelBackground()
        val s = JBScrollPane(jsonView)
        s.border = BorderFactory.createEmptyBorder()
        background = UIUtil.getPanelBackground()
        addToCenter(s)
    }


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?) {
        if (json != null && !project.isDisposed) {
            val changeJson = changeJson(json)
            WriteCommandAction.runWriteCommandAction(project) {
                jsonView.text = changeJson
            }
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

