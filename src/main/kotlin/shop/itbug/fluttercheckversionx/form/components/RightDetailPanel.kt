package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
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
class RightDetailPanel(project: Project) : JPanel() {


    /**
     * 详情对象
     */
    private var detail: Request? = null

    /**
     * json视图
     */
    private var jsonView: JsonValueRender


    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder()
        jsonView = JsonValueRender(jsonObject = "", project = project)
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
        val jbScrollPane = JBScrollPane(jsonView)
        jbScrollPane.isOpaque = true
        jbScrollPane.border = BorderFactory.createEmptyBorder()
        add(jbScrollPane, BorderLayout.CENTER)
    }

    /**
     * 清空显示
     */
    fun clean() {
        jsonView.changeValue("")
    }


}