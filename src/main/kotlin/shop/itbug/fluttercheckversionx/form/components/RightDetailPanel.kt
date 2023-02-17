package shop.itbug.fluttercheckversionx.form.components

import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.socket.create
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.MyActionButton
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

    private val actionsToolBar : BorderLayoutPanel get() {
        val defaultGroup = DefaultActionGroup(*createAnActions())
        return BorderLayoutPanel().apply {
            addToTop(defaultGroup.create("Request Json Toolbar").component)
        }
    }

    private fun createAnActions() : Array<AnAction> {
        return arrayOf(
            jsonToFreezedModelAction().action
        )
    }



    private fun jsonToFreezedModelAction() : ActionButton = MyActionButton(object  : DumbAwareAction({""},MyIcons.freezed) {
        override fun actionPerformed(e: AnActionEvent) {
            jsonToFreezedModel()
        }
    })



    /**
     * 将json转成freezed模型对象
     */
    private fun jsonToFreezedModel() {
        val text = jsonView.text.trim()
        if(text.isEmpty()){
            project.toastWithError(PluginBundle.get("input.your.json"))
            return
        }
        try {
            JSONObject.parseObject(jsonView.text)
        }catch (e:Exception){
            project.toastWithError(PluginBundle.get("json.format.verification.failed"))
            return
        }
        project.jsonToFreezedRun(text)
    }


}


