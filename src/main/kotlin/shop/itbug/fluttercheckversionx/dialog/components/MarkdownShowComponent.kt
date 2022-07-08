package shop.itbug.fluttercheckversionx.dialog.components

import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.document.MarkdownRender
import javax.swing.JTextPane

class MarkdownShowComponent(value: String, project: Project) : JTextPane() {

    lateinit var myProject: Project
    init {
        myProject = project
        isEditable = false
        contentType = "text/html"
        background = UIUtil.getPanelBackground()
        text = MarkdownRender.renderText(value,project)
    }

    /**
     * 更新文本
     */
    fun changeMarkdown(markdownText:String){
        text = MarkdownRender.renderText(markdownText, project = myProject )
    }

}