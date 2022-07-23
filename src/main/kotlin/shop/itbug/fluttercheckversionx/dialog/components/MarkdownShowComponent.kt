package shop.itbug.fluttercheckversionx.dialog.components

import com.intellij.codeInsight.documentation.DocumentationComponent
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.document.Helper
import shop.itbug.fluttercheckversionx.document.MarkdownRender
import javax.swing.JComponent
import javax.swing.JTextPane

class MarkdownShowComponent(value: String, project: Project, private var igToHtml: Boolean = false) : JTextPane() {

    private var myProject: Project
    init {
        myProject = project
        isEditable = false
        contentType = "text/html"
        background = UIUtil.getPanelBackground()
        text = if(igToHtml) value else MarkdownRender.renderText(value,project)
    }

    /**
     * 更新文本
     */
    fun changeMarkdown(markdownText:String){
        text = MarkdownRender.renderText(markdownText, project = myProject )


    }

    companion object {

        /**
         * @param conext html内容
         * @param htmlText 是否需要markdown转成html true 表示需要 false 表示不需要
         */
        fun getDocComp(conext: String,myProject: Project,htmlText: Boolean = true): DocumentationComponent{
            val obj = DocumentationComponent(DocumentationManager(myProject))
            obj.setText(if (htmlText) conext else  MarkdownRender.renderText(conext, project = myProject ),null,null)
            obj.background = UIUtil.getPanelBackground()
            return  obj
        }
    }
}

fun DocumentationComponent.changeMarkdownText(markdownText: String,project: Project){
    this.setText(Helper.markdown2Html(markdownText,project),null,null)
}