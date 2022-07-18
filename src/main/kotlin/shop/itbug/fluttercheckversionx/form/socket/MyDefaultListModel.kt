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

    fun getReverseDataList(): List<Request> {
        val copy = datas.toMutableList()
        copy.reverse()
        return copy
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

        val box = Box.createHorizontalBox()

        box.background = UIUtil.getListBackground()
        val url = UrlBuilder.ofHttp(value!!.url)

        ///最外层的panel
        val rootPanel = JPanel()
        rootPanel.layout = BorderLayout()


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
        methedLabel.text = "[${value.methed}]"
        methedLabel.foreground = Color.GRAY
        urlPanel.add(methedLabel)

        //状态码
        val statusCodeLabel = JLabel()
        val statusCode = value.statusCode.toString()
        statusCodeLabel.text = statusCode
        if (statusCode != "200") {
            statusCodeLabel.foreground = Color.RED
        } else {
            statusCodeLabel.foreground = ColorUtil.fromHex("#79bf2d")
        }
        urlPanel.add(statusCodeLabel)


        urlPanel.minimumSize = Dimension(200, 0)
        centerPanel.add(urlPanel)

        rootPanel.add(centerPanel, BorderLayout.CENTER)
        rootPanel.add(JLabel(AllIcons.Javaee.WebService), BorderLayout.LINE_START)

        if (isSelected) {
            rootPanel.background = UIUtil.getListSelectionBackground(false)
            urlPanel.background = UIUtil.getListSelectionBackground(false)
            box.background = UIUtil.getListSelectionBackground(false)

        }

        box.add(rootPanel)
        box.add(Box.createHorizontalGlue())


        ///请求耗时label
        val timerLabel = JLabel()
        timerLabel.text = (value.timesatamp).toString() + "毫秒"
        timerLabel.foreground = Color.GRAY
        if (isSelected) {
            timerLabel.background = UIUtil.getListSelectionBackground(false)
        } else {
            timerLabel.background = UIUtil.getListBackground()
        }
        box.add(timerLabel)
        return box
    }
}

