package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DioRequestUIStyle
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.form.socket.Request
import java.awt.Dimension
import java.net.URI
import javax.swing.BorderFactory


//获取 URL 显示
fun Request.formatUrl(setting: DoxListeningSetting): String {
    val uri = URI(url ?: "")
    val host = uri.host
    val scheme = uri.scheme + "://"

    val param = uri.rawQuery
    var string = this.url ?: ""
    if (setting.showHost.not()) {
        string = string.replace(scheme, "").replace(host, "")
        if (string.startsWith(":${uri.port}")) {
            string = string.replace(":${uri.port}", "")
        }
    }
    if (setting.showQueryParams.not()) {
        string = string.replace("?$param", "")

    }
    return string
}

//fun Request.getMethodColor(): Color? {
//    return when (method?.uppercase(Locale.getDefault())) {
//        "POST" -> JBColor.BLUE
//        "GET" -> JBColor.GREEN
//        "PUT" -> JBColor.CYAN
//        "DELETE" -> JBColor.RED
//        else -> null
//    }
//}

/**
 * 新版的请求UI
 */
fun requestDetailLayout(request: Request, isSelected: Boolean): DialogPanel {

    val setting = DioListingUiConfig.setting

    val color = UIUtil.getLabelDisabledForeground()


    val p = panel {

        row {

            // (在紧凑模式下有效,请求方式)
            label(
                request.method ?: ""
            ).visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showMethod).component.apply {
                font = JBFont.small()
                foreground = color
                minimumSize = Dimension(30, minimumSize.height)
//                background = request.getMethodColor()
//                isOpaque = true
            }
            // (在紧凑模式下有效,状态码)
            label("${request.statusCode ?: -1}").visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showStatusCode).component.apply {
                font = JBFont.medium()
                foreground =
                    if (request.statusCode == 200) UIUtil.getLabelSuccessForeground() else UIUtil.getErrorForeground()
            }

            label(request.formatUrl(setting)).component.apply {
                foreground = if (isSelected) UIUtil.getListSelectionForeground(false) else UIUtil.getLabelForeground()
                if (setting.urlBold) {
                    font = JBFont.medium().asBold()
                }
            }

            ///扩展备注
            request.extendNotes.map {
                label(it).component.apply {
                    font = JBFont.small()
                    foreground = color
                }
            }

            //其他一些次要的 (在紧凑模式下显示)
            label(request.timestamp!!.toString() + "ms").visible(setting.showTimestamp && setting.uiStyle == DioRequestUIStyle.CompactStyle).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.createDate).visible(setting.showDate && setting.uiStyle == DioRequestUIStyle.CompactStyle).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(
                request.projectName
            ).visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showProjectName).component.apply {
                font = JBFont.small()
                foreground = color
            }
        }

        //如果是紧凑模式,这些数据就不要显示了
        if (setting.uiStyle == DioRequestUIStyle.DefaultStyle) row {
            label(request.statusCode!!.toString()).visible(setting.showStatusCode).component.apply {
                font = JBFont.small()
                foreground =
                    if (request.statusCode == 200) UIUtil.getLabelInfoForeground() else UIUtil.getErrorForeground()
            }
            label(request.method!!).visible(setting.showMethod).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.timestamp!!.toString() + "ms").visible(setting.showTimestamp).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.createDate).visible(setting.showDate).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.projectName).visible(setting.showProjectName).component.apply {
                font = JBFont.small()
                foreground = color
            }
        }.visible(setting.showStatusCode || setting.showMethod || setting.showTimestamp || setting.showDate || setting.showProjectName)
    }
    p.background = if (isSelected) UIUtil.getListBackground(true, false) else UIUtil.getPanelBackground()
    return p.withBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12))
}

