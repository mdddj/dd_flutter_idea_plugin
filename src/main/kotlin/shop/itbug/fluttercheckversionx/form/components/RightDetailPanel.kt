package shop.itbug.fluttercheckversionx.form.components

import cn.hutool.core.lang.Console
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.common.toJsonFormart
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.services.impl.ModelToFreezedModelServiceImpl
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * 请求详情
 * ```dart
 * print("hello world");
 * ```
 */
class RightDetailPanel(project: Project) : JPanel(BorderLayout()) {


    private val freezedService = ModelToFreezedModelServiceImpl()
    /**
     * 详情对象
     */
    private var detail: Request? = null

    /**
     * json视图
     */
    private var jsonView: JsonValueRender = JsonValueRender( project = project)


    init {
        border = BorderFactory.createEmptyBorder()
        jsonViewInit()
        add(actionsToolBar,BorderLayout.NORTH)
    }


    /**
     * 显示详情信息
     */
    fun changeShowValue(detail: Request) {
        this.detail = detail
        jsonView.changeValue(detail.data)
    }

    private fun jsonViewInit() {
        add(jsonView, BorderLayout.CENTER)
    }

    /**
     * 清空显示
     */
    fun clean() {
        jsonView.changeValue("")
    }

    private val actionsToolBar : DialogPanel get() {
         return panel {
            row {
                button("json转freezed模型") {
                    jsonToFreezedModel()
                }
            }
        }
    }


    /**
     * 将json转成freezed模型对象
     */
    private fun jsonToFreezedModel() {
        val jsonObject = JSONObject.parseObject(jsonView.text)
        val jsonObjectToFreezedCovertModelList = freezedService.jsonObjectToFreezedCovertModelList(jsonObject)
        Console.log("转模型:${jsonObjectToFreezedCovertModelList.toJsonFormart()}")

    }




}


