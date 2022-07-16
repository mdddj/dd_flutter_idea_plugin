package shop.itbug.fluttercheckversionx.form.sub

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.myjsonview.json.gui.JsonViewerPanel
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */

class JsonValueRender(private val jsonObject: Any, var project: Project) : JPanel() {


    private var jsonView : JsonViewerPanel = JsonViewerPanel.instance

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder()
        add(jsonView, BorderLayout.CENTER)
    }


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?) {
        if (json != null) {
            val changeJson = changeJson(json)
            jsonView.changeText(changeJson)
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
            JSON.toJSONString(
                JSON.parseObject(
                    json as String
                ),
                JSONWriter.Feature.PrettyFormat
            )
        } else {
            JSON.toJSONString(json, JSONWriter.Feature.PrettyFormat)
        }
    }


}