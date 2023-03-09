package shop.itbug.fluttercheckversionx.form.components

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.common.MyDumbAwareAction
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.socket.createWithToolbar
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.toastWithError
import shop.itbug.fluttercheckversionx.widget.MyActionButton
import shop.itbug.fluttercheckversionx.widget.WidgetUtil
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
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
    private var jsonView: JsonValueRender = JsonValueRender(project = project)


    init {
        border = BorderFactory.createEmptyBorder()
        jsonViewInit()
        add(actionsToolBar, BorderLayout.NORTH)
        FlutterApiClickBus.listening {
            changeShowValue(it)
        }
    }


    /**
     * 显示详情信息
     */
    private fun changeShowValue(detail: Request) {
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

    private val actionsToolBar: JComponent
        get() {
            val toolbar = DefaultActionGroup(*createAnActions).createWithToolbar("Request Json Toolbar")
            toolbar.targetComponent = this
            return toolbar.component
        }

    private val createAnActions
        get() : Array<AnAction> {
            return arrayOf(
                MyActionButton(jsonToFreezedModelAction).action,
                WidgetUtil.getCopyAnAction(jsonView.text)
            )
        }

    private val jsonToFreezedModelAction: MyDumbAwareAction
        get() =
            object : MyDumbAwareAction("Json To Freezed Model","将json转换成freezed 模型",MyIcons.freezed) {
                override fun actionPerformed(e: AnActionEvent) {
                    jsonToFreezedModel()
                }

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = jsonView.text.trim().isNotEmpty() && JSON.isValid(jsonView.text)
                    super.update(e)
                }

            }

    /**
     * 将json转成freezed模型对象
     */
    private fun jsonToFreezedModel() {
        val text = jsonView.text.trim()
        if (text.isEmpty()) {
            project.toastWithError(PluginBundle.get("input.your.json"))
            return
        }
        try {
            JSONObject.parseObject(jsonView.text)
        } catch (e: Exception) {
            project.toastWithError(PluginBundle.get("json.format.verification.failed"))
            return
        }
        project.jsonToFreezedRun(text)
    }


}


