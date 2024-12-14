package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBLabel
import shop.itbug.fluttercheckversionx.dsl.requestDetailLayout
import java.awt.Component
import javax.swing.JList

///渲染请求列表
class MyCustomItemRender : ColoredListCellRenderer<Request>() {

    override fun getListCellRendererComponent(
        list: JList<out Request>?,
        value: Request?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ): Component {
        if (value == null) {
            return JBLabel()
        }

        val dialog = requestDetailLayout(value, selected)
        return dialog
    }

    override fun customizeCellRenderer(
        list: JList<out Request?>,
        value: Request?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        
    }

}




