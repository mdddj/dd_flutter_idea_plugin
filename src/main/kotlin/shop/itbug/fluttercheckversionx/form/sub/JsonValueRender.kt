package shop.itbug.fluttercheckversionx.form.sub

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
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


    private lateinit var jsonView: LanguageTextField

    init {

        layout = BorderLayout(0, 12)
        border = BorderFactory.createEmptyBorder()

        //创建展示json区域
        createJsonEditer()
        add(jsonView, BorderLayout.CENTER)

    }


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?) {
        if (json != null) {
            val changeJson = changeJson(json)
            jsonView.text = changeJson
        } else {
            jsonView.text = ""
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

    /**
     * 创建一个json viewer
     */
    private fun createJsonEditer() {

        jsonView = LanguageTextField(JsonLanguage.INSTANCE, project, changeJson(jsonObject), false)
        jsonView.border = BorderFactory.createEmptyBorder()

    }
}