package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
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
    }


    /**
     * 显示详情信息
     */
    fun changeShowValue(detail: Request) {
        this.detail = detail
        jsonView.changeValue(detail.body)
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


}