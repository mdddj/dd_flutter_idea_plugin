package shop.itbug.fluttercheckversionx.form.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import shop.itbug.fluttercheckversionx.form.RequestDetailForm
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import shop.itbug.fluttercheckversionx.util.ScreenUtil
import javax.swing.JComponent


///请求的详细弹窗
class DetailDialog(var project: Project, private val detail: ProjectSocketService.SocketResponseModel) :
    DialogWrapper(project) {

    init {
        init()
    }


    override fun setSize(width: Int, height: Int) {
        val screenSize = ScreenUtil.getScreenSize()
        super.setSize(screenSize.width-200, screenSize.height-200)
    }



    override fun createCenterPanel(): JComponent? {
        return RequestDetailForm(detail,project).content.rootPane
    }

    override fun setTitle(title: String?) {
        super.setTitle("请求详情")
    }
}