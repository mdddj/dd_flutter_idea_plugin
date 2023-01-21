package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.icons.MyIcons
import javax.swing.BorderFactory

/**
 * 新版的请求UI
 */
fun requestDetailLayout(request: Request, isSelected: Boolean): DialogPanel {

    val color = UIUtil.getLabelDisabledForeground()
    val p = panel {
        row {
            cell(JBLabel(MyIcons.apiIcon))
            label(request.url!!).bold().component.apply {
                foreground = if(isSelected) UIUtil.getListSelectionForeground(false) else UIUtil.getLabelForeground()
            }
        }
        row {
            label(request.statusCode!!.toString()).component.apply {
                font = JBFont.small()
                foreground = if(request.statusCode == 200) UIUtil.getLabelInfoForeground() else color
            }
            label(request.method!!).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.timestamp!!.toString()+"ms").component.apply {
                font = JBFont.small()
                foreground = color
            }
        }
    }
    p.background = if(isSelected)  UIUtil.getListBackground(true,false)  else UIUtil.getPanelBackground()
    return p.withBorder(BorderFactory.createEmptyBorder(0,12,0,12))
}