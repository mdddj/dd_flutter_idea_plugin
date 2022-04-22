package form.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import form.RequestDetailForm
import socket.ProjectSocketService
import util.ScreenUtil
import javax.swing.JComponent


///请求的详细弹窗
class DetailDialog(project: Project?, private val detail: ProjectSocketService.SocketResponseModel) :
    DialogWrapper(project) {

    init {
        init()
    }


    override fun setSize(width: Int, height: Int) {
        val screenSize = ScreenUtil.getScreenSize()
        super.setSize(screenSize.width-200, screenSize.height-200)
    }


    override fun createCenterPanel(): JComponent? {
        return RequestDetailForm(detail).content
    }

    override fun setTitle(title: String?) {
        super.setTitle("请求详情")
    }
}