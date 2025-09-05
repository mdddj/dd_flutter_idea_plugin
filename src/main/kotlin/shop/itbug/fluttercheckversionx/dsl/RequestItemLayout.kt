package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig
import shop.itbug.fluttercheckversionx.config.DioRequestUIStyle
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.model.calculateSize
import shop.itbug.fluttercheckversionx.socket.Request
import shop.itbug.fluttercheckversionx.util.MyFontUtil
import java.awt.Dimension
import java.net.URI
import java.util.regex.Pattern
import javax.swing.BorderFactory

fun extractIpAddressFromUrl(url: String): String {
    val pattern = Pattern.compile("(?<=http[s]?://)(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?::\\d+)?")
    val matcher = pattern.matcher(url)
    return if (matcher.find()) matcher.group(1) else ""
}


fun removePortFromUrl(url: String): String {
    // 正则表达式匹配":数字/"，其中\d+表示一个或多个数字
    val pattern = Pattern.compile(":\\d+/")
    val matcher = pattern.matcher(url)
    // 使用replaceFirst函数替换第一个匹配到的部分为空字符串，实现删除效果
    return matcher.replaceFirst("/")
}

//获取 URL 显示
fun Request.formatUrl(setting: DoxListeningSetting): String {
    val uri = URI(url)
    var host = uri.host ?: ""
    val scheme = uri.scheme + "://"
    if (host.isEmpty()) {
        host = extractIpAddressFromUrl(url)
    }
    val param = uri.rawQuery
    var string = this.url
    if (setting.showHost.not()) {
        string = string.replace(host, "").replace(scheme, "")
        if (string.startsWith(":${uri.port}")) {
            string = string.replace(":${uri.port}", "")
        } else if (uri.port == -1) {
            string = removePortFromUrl(string)
        }
    }
    if (setting.showQueryParams.not()) {
        string = string.replace("?$param", "")
    }
    string = string.replace("&", "\u0026")
    return string
}


/**
 * 新版的请求UI
 */
fun requestDetailLayout(request: Request, isSelected: Boolean): DialogPanel {

    val setting = DioListingUiConfig.setting

    val color = UIUtil.getLabelDisabledForeground()
//    val icon = TextIcon((request.method ?: "TT").toLowerCase(), JBColor.foreground(), JBColor.background(), 2, true)


    val p = panel {

        row {

//            icon(icon)
            // (在紧凑模式下有效,请求方式)
            label(
                request.method ?: ""
            ).visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showMethod).component.apply {
                font = JBFont.small()
                foreground = color
                minimumSize = Dimension(MyFontUtil.getRequestLayoutRenderMethodMinWidth(), minimumSize.height)
//                background = request.getMethodColor()
//                isOpaque = true
            }
            // (在紧凑模式下有效,状态码)
            label("${request.statusCode}").visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showStatusCode).component.apply {
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

            label(request.calculateSize()).visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showDataSize).component.apply {
                font = JBFont.small()
                foreground = color
            }

            label(request.timestamp.toString() + "ms").visible(setting.showTimestamp && setting.uiStyle == DioRequestUIStyle.CompactStyle).component.apply {
                font = JBFont.small()
                foreground = color
            }
            request.createDate.let {
                label(it).visible(setting.showDate && setting.uiStyle == DioRequestUIStyle.CompactStyle).component.apply {
                    font = JBFont.small()
                    foreground = color
                }
            }
            request.projectName.let {
                label(
                    it
                ).visible(setting.uiStyle == DioRequestUIStyle.CompactStyle && setting.showProjectName).component.apply {
                    font = JBFont.small()
                    foreground = color
                }
            }

        }

        //如果是紧凑模式,这些数据就不要显示了
        if (setting.uiStyle == DioRequestUIStyle.DefaultStyle) row {
            label(request.statusCode.toString()).visible(setting.showStatusCode).component.apply {
                font = JBFont.small()
                foreground =
                    if (request.statusCode == 200) UIUtil.getLabelInfoForeground() else UIUtil.getErrorForeground()
            }
            label(request.method!!).visible(setting.showMethod).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.calculateSize()).visible(setting.showDataSize).component.apply {
                font = JBFont.small()
                foreground = color
            }
            label(request.timestamp.toString() + "ms").visible(setting.showTimestamp).component.apply {
                font = JBFont.small()
                foreground = color
            }
            request.createDate.let {
                label(it).visible(setting.showDate).component.apply {
                    font = JBFont.small()
                    foreground = color
                }
            }
            request.projectName.let {
                label(it).visible(setting.showProjectName).component.apply {
                    font = JBFont.small()
                    foreground = color
                }
            }

        }.visible(setting.showStatusCode || setting.showMethod || setting.showTimestamp || setting.showDate || setting.showProjectName || setting.showDataSize)
    }
    p.background = if (isSelected) UIUtil.getListBackground(true, false) else UIUtil.getPanelBackground()
    return p.withBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12))
}

