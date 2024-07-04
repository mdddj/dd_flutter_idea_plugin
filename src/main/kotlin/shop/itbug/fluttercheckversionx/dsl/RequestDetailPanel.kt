package shop.itbug.fluttercheckversionx.dsl

import com.google.gson.Gson
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.form.socket.Request
import javax.swing.BorderFactory

///请求详情面板
fun requestDetailPanel(request: Request, project: Project): DialogPanel {


    val p = panel {
        row("Url") {
            label(request.url ?: "")
        }
        row("Method") {
            label(request.method ?: "")
        }
        row("Time") {
            label("${request.timestamp}ms").apply {
                if (request.timestamp > 2000) {
                    component.foreground = JBColor.ORANGE
                } else {
                    component.foreground = UIUtil.getLabelInfoForeground()
                }
            }
        }
        row("Status Code") {
            label("${request.statusCode}").apply {
                if (request.statusCode == 200) {
                    component.foreground = UIUtil.getLabelInfoForeground()
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
                    Gson().toJson(request.headers),
                    false
                ).apply {
                    border = BorderFactory.createEmptyBorder()
                }
            ).visible(request.headers?.isNotEmpty() == true)
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
                    Gson().toJson(request.responseHeaders),
                    false
                ).apply {
                    border = BorderFactory.createEmptyBorder()
                }
            ).visible(request.responseHeaders?.isNotEmpty() == true)
        }.visibleIf(box.selected)
    }
    return p
}