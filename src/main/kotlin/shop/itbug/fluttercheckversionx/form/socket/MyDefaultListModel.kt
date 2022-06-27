package shop.itbug.fluttercheckversionx.form.socket

import cn.hutool.core.net.url.UrlBuilder
import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*

class MyDefaultListModel(private val datas: List<Request>): AbstractListModel<Request>() {
    override fun getSize(): Int {
        return datas.size
    }

    override fun getElementAt(index: Int): Request {
        return datas[index]
    }
}

class MyCustomItemRender : ListCellRenderer<Request> {

    override fun getListCellRendererComponent(
        list: JList<out Request>?,
        value: Request?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {


        val url = UrlBuilder.ofHttp(value!!.url)


        ///最外层的panel
        val rootPanel = JPanel()
        rootPanel.layout = BorderLayout()


        /// 中间一层的panel
        val centerPanel = JPanel()
        centerPanel.layout = BoxLayout(centerPanel,BoxLayout.Y_AXIS)

        /// url + methed panel
        val urlPanel = JPanel(FlowLayout(FlowLayout.LEADING))

        //路径 label
        val urlLabel = JLabel()
        urlLabel.text = url.pathStr
        urlLabel.foreground = UIUtil.getLabelForeground()
        urlPanel.add(urlLabel)

        val timerLabel = JLabel()
        timerLabel.text = (value.timesatamp).toString() + "ms"
        timerLabel.foreground = Color.GRAY
        urlPanel.add(timerLabel)

        //请求方法 label
        val methedLabel = JLabel()
        methedLabel.text = "[${value.methed}]"
        methedLabel.foreground = Color.GRAY
        urlPanel.add(methedLabel)

        //状态码
        val statusCodeLabel = JLabel()
        val statusCode = value.statusCode.toString()
        statusCodeLabel.text = statusCode
        if(statusCode!="200"){
            statusCodeLabel.foreground = Color.RED
        }else {
            statusCodeLabel.foreground = ColorUtil.fromHex("#79bf2d")
        }
        urlPanel.add(statusCodeLabel)


        urlPanel.minimumSize = Dimension(200,0)
        centerPanel.add(urlPanel)

        rootPanel.add(centerPanel,BorderLayout.CENTER)
        rootPanel.add(JLabel(AllIcons.Javaee.WebService),BorderLayout.LINE_START)

        if(isSelected){
            rootPanel.background =  UIUtil.getListSelectionBackground(false)
            urlPanel.background = UIUtil.getListSelectionBackground(false)
        }

        return rootPanel
    }

}