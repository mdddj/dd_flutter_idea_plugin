package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout

class CommentListWidget: JBPanel<CommentListWidget>(BorderLayout()) {

    init {
        add(JBScrollPane(JBList<Any>()),BorderLayout.CENTER)
    }
}