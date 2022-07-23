package shop.itbug.fluttercheckversionx.form.socket

import cn.hutool.core.net.url.UrlBuilder
import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*

class MyDefaultListModel(private val datas: List<Request>) :
    AbstractListModel<Request>() {
    var list = datas


    override fun getSize(): Int {
        return list.size
    }

    override fun getElementAt(index: Int): Request {
        return list[index]
    }

}

///渲染请求列表
class MyCustomItemRender : ListCellRenderer<Request> {

    override fun getListCellRendererComponent(
        list: JList<out Request>?,
        value: Request?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (value == null) return JLabel("未知请求")
        return MyRequestItemPanel(value, isSelected)
    }
}


///请求列表item布局
class MyRequestItemPanel(request: Request, isSelected: Boolean) : JPanel() {
    init {
        val url = UrlBuilder.ofHttp(request.url)
        layout = BorderLayout()
        /// 中间一层的panel
        val centerPanel = JPanel()
        centerPanel.layout = BoxLayout(centerPanel, BoxLayout.Y_AXIS)

        /// url + methed panel
        val urlPanel = JPanel(FlowLayout(FlowLayout.LEADING))

        //路径 label
        val urlLabel = JLabel()
        urlLabel.text = url.pathStr
        urlLabel.foreground = UIUtil.getLabelForeground()
        urlPanel.add(urlLabel)


        //请求方法 label
        val methedLabel = JLabel()
        methedLabel.text = "[${request.methed}]"
        methedLabel.foreground = Color.GRAY
        urlPanel.add(methedLabel)

        //状态码
        val statusCodeLabel = JLabel()
        val statusCode = request.statusCode.toString()
        statusCodeLabel.text = statusCode
        if (statusCode != "200") {
            statusCodeLabel.foreground = Color.RED
        } else {
            statusCodeLabel.foreground = ColorUtil.fromHex("#79bf2d")
        }
        urlPanel.add(statusCodeLabel)


        urlPanel.minimumSize = Dimension(200, 0)
        centerPanel.add(urlPanel)

        add(centerPanel, BorderLayout.CENTER)
        add(JLabel(AllIcons.Javaee.WebService), BorderLayout.LINE_START)

        if (isSelected) {
            background = UIUtil.getListSelectionBackground(false)
            urlPanel.background = UIUtil.getListSelectionBackground(false)

        }


        ///请求耗时label
        val timerLabel = JLabel()
        timerLabel.text = (request.timesatamp).toString() + "毫秒"
        timerLabel.foreground = Color.GRAY
        if (isSelected) {
            timerLabel.background = UIUtil.getListSelectionBackground(false)
        } else {
            timerLabel.background = UIUtil.getListBackground()
        }
        add(timerLabel)
    }


}