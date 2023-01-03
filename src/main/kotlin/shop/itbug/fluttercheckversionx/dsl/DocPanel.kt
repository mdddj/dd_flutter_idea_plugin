package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.document.Helper
import shop.itbug.fluttercheckversionx.document.MyMarkdownDocRenderObject

///markdown渲染弹窗
fun docPanel(markdownText:String,project: Project,covert: Boolean = true) : DialogPanel {
    val mk = if(covert) Helper.markdown2Html(MyMarkdownDocRenderObject(markdownText,project)) else markdownText
    val p = panel {
        row {
            label("<html>${mk}</html>")
        }
    }
    return  p.addBorder()
}