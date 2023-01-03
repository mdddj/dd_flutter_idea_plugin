package shop.itbug.fluttercheckversionx.dialog.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import shop.itbug.fluttercheckversionx.dsl.docPanel
import java.awt.BorderLayout

class MarkdownShowComponent(value: String, project: Project) : JBPanel<MarkdownShowComponent>(BorderLayout()) {


    private var project: Project
    init {
        this@MarkdownShowComponent.project = project
        add(docPanel(value,project,false),BorderLayout.CENTER)
    }

    /**
     * 更新Markdown文本
     */
    fun changeMarkdown(markdownText: String) {
        this.removeAll()
        add(docPanel(markdownText,project,false),BorderLayout.CENTER)
    }


    companion object {
        fun getDocComp(content: String, myProject: Project): MarkdownShowComponent {
            return MarkdownShowComponent(content, myProject)
        }
    }
}

