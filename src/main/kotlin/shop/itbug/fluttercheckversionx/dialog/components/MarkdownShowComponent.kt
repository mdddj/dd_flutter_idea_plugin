package shop.itbug.fluttercheckversionx.dialog.components

import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.document.MarkdownRender
import javax.swing.JTextPane

class MarkdownShowComponent(value: String, project: Project, igToHtml: Boolean = false) : JTextPane() {

    private var myProject: Project

    init {
        myProject = project
        isEditable = false
        contentType = "text/html"
        background = UIUtil.getPanelBackground()
        text = if (igToHtml) value else MarkdownRender.renderText(value, project)
    }

    /**
     * 更新Markdown文本
     */
    fun changeMarkdown(markdownText: String) {
        text = MarkdownRender.renderText(markdownText, project = myProject)
    }

    fun changeHtml(htmlText: String) {
        text = htmlText
    }

    companion object {

        /**
         * @param conext html内容
         * @param htmlText 是否需要markdown转成html true 表示需要 false 表示不需要
         */
        fun getDocComp(conext: String, myProject: Project, htmlText: Boolean = true): MarkdownShowComponent {
            val obj = MarkdownShowComponent(conext, myProject ,htmlText)
            obj.text = if (htmlText) conext else MarkdownRender.renderText(conext, project = myProject)
            obj.autoscrolls=false
            return obj
        }
    }
}

