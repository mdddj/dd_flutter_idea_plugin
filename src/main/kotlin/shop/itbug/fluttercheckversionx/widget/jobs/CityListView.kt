package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.util.MyActionUtil
import shop.itbug.fluttercheckversionx.util.toolbar
import java.awt.BorderLayout


class CityListView(val project: Project,onListSelect: (category: ResourceCategory)->Unit) : JBPanel<CityListView>(BorderLayout()) {


    private val group = MyActionUtil.jobCityToolbarActionGroup

    private val toolbar = group.toolbar("城市列表")

    private val cityWidget = JobsCitySelectWidget(project)

    private val refresh = DumbAwareAction.create(){
        refreshList()
    }

    init {
        uiInit()
        toolbar.targetComponent = this
        add(toolbar.component, BorderLayout.NORTH)
        add(JBScrollPane(cityWidget), BorderLayout.CENTER)
        cityWidget.addListSelectionListener { e ->
            onListSelect.invoke(cityWidget.selectedValue).takeIf { !e.valueIsAdjusting }
        }
    }




    private fun uiInit() {
        group.add(refresh)
    }

    private fun refreshList(){
        cityWidget.refresh()
    }



}