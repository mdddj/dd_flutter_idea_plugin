package shop.itbug.fluttercheckversionx.dsl

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import shop.itbug.fluttercheckversionx.form.socket.Request
import javax.swing.BorderFactory

///请求详情面板
fun requestDetailPanel(request: Request, project: Project): DialogPanel {


    val p = panel {
        row("Url") {
            label(request.url)
        }
        row("Method") {
            label(request.method)
        }
        row("Time") {
            label("${request.timestamp}ms").apply {
                if (request.timestamp > 2000) {
                    component.foreground = JBColor.ORANGE
                } else {
                    component.foreground = JBColor.GREEN
                }
            }
        }
        row("Status Code") {
            label("${request.statusCode}").apply {
                if (request.statusCode == 200) {
                    component.foreground = JBColor.GREEN
                } else {
                    component.foreground = JBColor.RED
                }
            }
        }
        row("Headers") {
            scrollCell(
                LanguageTextField(
                    JsonLanguage.INSTANCE,
                    project,
                    JSONObject.toJSONString(request.headers, JSONWriter.Feature.PrettyFormat),
                    false
                ).apply {
                    border = BorderFactory.createEmptyBorder()
                }
            ).visible(request.headers.isNotEmpty())
        }
        lateinit var box: Cell<JBCheckBox>
        row("Response Headers") {
            box = checkBox("Show Response Header")
        }
        row {
            scrollCell(
                LanguageTextField(
                    JsonLanguage.INSTANCE,
                    project,
                    JSONObject.toJSONString(request.responseHeaders, JSONWriter.Feature.PrettyFormat),
                    false
                ).apply {
                    border = BorderFactory.createEmptyBorder()
                }
            ).visible(request.responseHeaders.isNotEmpty())
        }.visibleIf(box.selected)
    }
    return p
}