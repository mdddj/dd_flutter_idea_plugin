package shop.itbug.fluttercheckversionx.dialog.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.document.MarkdownRender
import javax.swing.JPanel
import javax.swing.JTextPane

class MarkdownShowComponent(value: String, project: Project, igToHtml: Boolean = false) : JBPanel<MarkdownShowComponent>() {

    private var myProject: Project


    init {
        myProject = project
        background = UIUtil.getPanelBackground()
    }

    /**
     * 更新Markdown文本
     */
    fun changeMarkdown(markdownText: String) {
        //todo
    }

    fun changeHtml(htmlText: String) {
    }

    companion object {

        /**
         * @param conext html内容
         * @param htmlText 是否需要markdown转成html true 表示需要 false 表示不需要
         */
        fun getDocComp(conext: String, myProject: Project, htmlText: Boolean = true): MarkdownShowComponent {
            val obj = MarkdownShowComponent(conext, myProject ,htmlText)
            obj.autoscrolls=false
            return obj
        }
    }
}

