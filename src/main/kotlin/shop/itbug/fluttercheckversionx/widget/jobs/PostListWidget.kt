package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.util.MyActionUtil
import shop.itbug.fluttercheckversionx.util.toolbar
import shop.itbug.fluttercheckversionx.widget.MyResourcesListWidget
import java.awt.BorderLayout

class PostListWidget(val project: Project) : JBPanel<PostListWidget>(BorderLayout()) {

    private val toolbar = MyActionUtil.jobPostToolbarActionGroup.toolbar("职位列表操作栏")
    private val listWidget: MyResourcesListWidget = MyResourcesListWidget(project)

    init {
        toolbar.targetComponent = this
        add(toolbar.component, BorderLayout.NORTH)
        add(JBScrollPane(listWidget), BorderLayout.CENTER)
    }



    fun changeListWithCategoryId(id: Long){
        listWidget.getJobsWithId(id)
    }


}