package shop.itbug.fluttercheckversionx.form.sub

import com.alibaba.fastjson.JSON
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.myjsonview.json.gui.JsonViewerPanel
import java.awt.BorderLayout
import java.io.IOException
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


    private var jsonView: JsonViewerPanel = JsonViewerPanel.instance

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
        val gsonBuilder =
            GsonBuilder()
                .create()

        val isJson = if (json is String) JSON.isValid(json) else false
        return if (isJson) {
            return gsonBuilder.toJson(gsonBuilder.fromJson(json.toString(), Map::class.java))
        } else {
            try {
                gsonBuilder.toJson(json)
            } catch (_: Exception) {
                json.toString()
            }
        }
    }


}


class DataTypeAdapter : TypeAdapter<Any?>() {
    private val delegate: TypeAdapter<Any> = Gson().getAdapter(Any::class.java)

    @Throws(IOException::class)
    override fun read(`in`: JsonReader): Any? {
        return when (`in`.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                `in`.beginArray()
                while (`in`.hasNext()) {
                    list.add(read(`in`))
                }
                `in`.endArray()
                list
            }
            JsonToken.BEGIN_OBJECT -> {
                val map: Map<String, Any?> = LinkedTreeMap()
                `in`.beginObject()
                while (`in`.hasNext()) {
                    map.plus(Pair(`in`.nextName(), read(`in`)))
                }
                `in`.endObject()
                map
            }
            JsonToken.STRING -> `in`.nextString()
            JsonToken.NUMBER -> {
                /**
                 * 改写数字的处理逻辑，将数字值分为整型与浮点型。
                 */
                val dbNum: Double = `in`.nextDouble()
                println("进来了:$dbNum")

                // 数字超过long的最大值，返回浮点类型
                if (dbNum > Long.MAX_VALUE) {
                    return dbNum
                }
                // 判断数字是否为整数值
                val lngNum = dbNum.toLong()
                if (dbNum == lngNum.toDouble()) {
                    try {
                        lngNum.toInt()
                    } catch (e: Exception) {
                        println("解析int失败:$e")
                        lngNum
                    }
                } else {
                    dbNum
                }
            }
            JsonToken.BOOLEAN -> `in`.nextBoolean()
            JsonToken.NULL -> {
                `in`.nextNull()
                null
            }
            else -> throw IllegalStateException()
        }
    }

    @Throws(IOException::class)
    override fun write(out: JsonWriter?, value: Any?) {
        delegate.write(out, value)
    }
}
