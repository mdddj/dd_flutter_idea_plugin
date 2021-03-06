package shop.itbug.fluttercheckversionx.form.sub

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
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

class JsonValueRender(var project: Project) : JPanel(BorderLayout()) {


    private var jsonView: LanguageTextField = LanguageTextField(
        JsonLanguage.INSTANCE,
        project,
        "",
        false
    )

    init {
        border = BorderFactory.createEmptyBorder()
        jsonView.border = BorderFactory.createEmptyBorder()
        val s = JBScrollPane(jsonView)
        s.border = BorderFactory.createEmptyBorder()
        add(s, BorderLayout.CENTER)
    }


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?) {
        if (json != null) {
            val changeJson = changeJson(json)
            jsonView.text = changeJson
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
            return JSON.toJSONString(JSON.parseObject(json.toString(), Map::class.java),SerializerFeature.PrettyFormat)
        } else {
            try {
                JSON.toJSONString(json,SerializerFeature.PrettyFormat)
            } catch (_: Exception) {
                json.toString()
            }
        }
    }


}

