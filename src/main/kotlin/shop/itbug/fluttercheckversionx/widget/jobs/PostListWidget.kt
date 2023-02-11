package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.util.MyActionUtil
import shop.itbug.fluttercheckversionx.util.toolbar
import java.awt.BorderLayout

class PostListWidget : JBPanel<PostListWidget>(BorderLayout()) {


    private val toolbar = MyActionUtil.jobPostToolbarActionGroup.toolbar("职位列表操作栏")


    init {
        add(toolbar.component, BorderLayout.NORTH)
        add(JBScrollPane(JBList<Any>()), BorderLayout.CENTER)
        initUi()
    }


    private fun initUi() {

    }
}