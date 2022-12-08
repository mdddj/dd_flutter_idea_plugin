package shop.itbug.fluttercheckversionx.dialog.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import shop.itbug.fluttercheckversionx.dsl.docPanel
import java.awt.BorderLayout

class MarkdownShowComponent(value: String, project: Project) : JBPanel<MarkdownShowComponent>(BorderLayout()) {


    private var panel = docPanel(value,project)
    private var project: Project
    init {
        this@MarkdownShowComponent.project = project
        add(panel,BorderLayout.CENTER)
    }

    /**
     * 更新Markdown文本
     */
    fun changeMarkdown(markdownText: String) {
        remove(panel)
        panel = docPanel(markdownText,project)
        add(panel,BorderLayout.CENTER)
    }


    companion object {

        /**
         * @param content html内容
         */
        fun getDocComp(content: String, myProject: Project): MarkdownShowComponent {
            return MarkdownShowComponent(content, myProject)
        }
    }
}

