package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender

/**
 * 请求详情
 * ```dart
 * print("hello world");
 * ```
 */
class RightDetailPanel(val project: Project) : BorderLayoutPanel() {

    /**
     * 详情对象
     */
    private var detail: Request? = null

    /**
     * json视图
     */
    private var jsonView: JsonValueRender = JsonValueRender(p = project)


    init {
        border = null
        jsonViewInit()
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
        addToCenter(jsonView)
    }

    /**
     * 清空显示
     */
    fun clean() {
        jsonView.changeValue("")
    }



    fun getText() = jsonView.text



}


