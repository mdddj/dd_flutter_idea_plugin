package shop.itbug.fluttercheckversionx.form.socket

import cn.hutool.core.net.url.UrlBuilder
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.icons.MyIcons
import java.awt.Component
import javax.swing.*

class MyDefaultListModel(datas: List<Request>) :
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
class MyRequestItemPanel(request: Request, isSelected: Boolean) : Box(BoxLayout.X_AXIS) {
    init {
        //icon
        add(JLabel(MyIcons.apiIcon))
        add(createHorizontalStrut(4))

        UrlBuilder.ofHttp(request.url)

        //path
        val pathLabel = JLabel(request.url).apply {
            foreground = UIUtil.getLabelForeground()
        }
        add(pathLabel)
        add(createHorizontalStrut(4))


        //method
        val methodLabel = JLabel(request.method).apply {
            foreground = JBColor.GRAY
        }
        add(methodLabel)
        add(createHorizontalStrut(4))

        //状态码
        val codeLabel = JLabel("${request.statusCode}").apply {
            foreground = if (request.statusCode != 200) {
                JBColor.RED
            } else {
                ColorUtil.fromHex("#79bf2d")
            }
        }
        add(codeLabel)
        add(createHorizontalStrut(4))



        if (isSelected) {
            background = UIUtil.getListSelectionBackground(false)
        }


        ///请求耗时label
        val timerLabel = JLabel((request.timestamp).toString() + "毫秒")
        timerLabel.foreground = JBColor.GRAY
        if (isSelected) {
            timerLabel.background = UIUtil.getListSelectionBackground(false)
        } else {
            timerLabel.background = UIUtil.getListBackground()
        }
        add(timerLabel)
    }


}