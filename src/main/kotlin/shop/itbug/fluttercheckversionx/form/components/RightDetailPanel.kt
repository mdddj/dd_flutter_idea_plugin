package shop.itbug.fluttercheckversionx.form.components

import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * 请求详情
 * ```dart
 * print("hello world");
 * ```
 */
class RightDetailPanel(val project: Project) : JPanel(BorderLayout()) {


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
        lateinit var p : DialogPanel
         p = panel {
            row {
                button(PluginBundle.get("freezed.btn.text")) {
                    println("转换...")
                    p.apply()
                    p.validate()
                    if(p.isValid){
                        jsonToFreezedModel()
                    }else{
                        println("验证不通过")
                    }
                }.validation{
                    if(jsonView.text.trim().isEmpty()){
                        ValidationInfoBuilder(JBLabel()).error(PluginBundle.get("input.your.json"))
                    }
                    try {
                        JSONObject.parseObject(jsonView.text)
                    }catch (e:Exception){
                        ValidationInfoBuilder(JBLabel()).error(PluginBundle.get("json.format.verification.failed"))
                    }
                    null
                }
            }
        }
        return p
    }




    /**
     * 将json转成freezed模型对象
     */
    private fun jsonToFreezedModel() {
        val text = jsonView.text.trim()
        if(text.isEmpty()){
            return
        }
        project.jsonToFreezedRun(text)
    }


}


