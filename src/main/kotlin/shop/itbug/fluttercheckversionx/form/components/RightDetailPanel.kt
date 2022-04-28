package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * 请求详情
 */
class RightDetailPanel: JPanel() {


    /**
     * 详情对象
     */
    private var detail: Request? = null

    /**
     * json视图
     */
    private var jsonView: JsonValueRender? = null



    init {
        layout = BorderLayout()
    }


    /**
     * 显示详情信息
     */
    fun changeShowValue(detail: Request, project: Project){
        this.detail = detail
        if(jsonView==null){
            jsonViewInit(project)
        }
        jsonView!!.changeValue(detail.body)


    }

    private fun jsonViewInit(project: Project){
        jsonView = JsonValueRender(detail?.body!!,project)
        add(JBScrollPane(jsonView),BorderLayout.CENTER)
    }



}