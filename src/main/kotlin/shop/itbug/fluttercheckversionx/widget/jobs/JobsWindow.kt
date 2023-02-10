package shop.itbug.fluttercheckversionx.widget.jobs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout

class JobsWindow (val project: Project, private val toolWindow: ToolWindow) : JBPanel<JobsWindow>(GridBagLayout()) {

    private val cityList = CityListView(project)

    init {
        val listConstraints = GridBagConstraints().apply {
            fill = GridBagConstraints.VERTICAL
            gridwidth = 200
            weightx = 1.0
        }
        add(cityList,listConstraints)
    }


}