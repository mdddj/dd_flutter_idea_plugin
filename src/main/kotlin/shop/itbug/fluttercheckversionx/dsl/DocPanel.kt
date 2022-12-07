package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.document.Helper
import shop.itbug.fluttercheckversionx.document.MyMarkdownDocRenderObject

///markdown渲染弹窗
fun docPanel(markdownText:String,project: Project) : DialogPanel {
    val p = panel {
        row {
            label("<html>${Helper.markdown2Html(MyMarkdownDocRenderObject(markdownText,project))}</html>")
        }
    }
    return  p.addBorder()
}