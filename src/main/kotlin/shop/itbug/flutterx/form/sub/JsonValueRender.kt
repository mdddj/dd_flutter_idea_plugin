package shop.itbug.flutterx.form.sub

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import shop.itbug.flutterx.socket.service.DioApiService
import shop.itbug.flutterx.widget.JsonEditorTextPanel


/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */

open class JsonValueRender(p: Project) : JsonEditorTextPanel(p) {

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

    private fun isValidJson(jsonString: String?): Boolean {
        try {
            JsonParser.parseString(jsonString)
            return true
        } catch (_: JsonSyntaxException) {
            return false
        }
    }

    /**
     * 改变显示内容
     *
     * 返回要显示的json string
     */
    private fun changeJson(json: Any): String {
        val builder = DioApiService.getInstance().gson

        if (json is String) {
            try {
                if (isValidJson(json)) {
                    val map = builder.fromJson(json, Map::class.java)
                    return builder.toJson(map)
                }
                if (json.toIntOrNull() != null) {
                    return "${json.toInt()}"
                }
            } catch (e: Exception) {
                println("尝试String转json失败:${e.localizedMessage} \n String is $json")
            }
        }
        if (json is Map<*, *>) {
            try {
                return builder.toJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (json is Int) {
            return "$json"
        }
        if (json is Double) {
            return "$json"
        }
        try {
            return builder.toJson(json)
        } catch (_: Exception) {
        }
        return json.toString()
    }

}


