package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.project.Project
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.widget.AddPackageDialogIdea

fun ActionGroup.toolbar(place: String): ActionToolbar {
    return ActionManager.getInstance().createActionToolbar(place, this, true)
}


object MyActionUtil {
    fun showPubSearchDialog(project: Project,yamlFile: YAMLFile? = null)  {
        val file = (yamlFile ?: MyFileUtil.getPubspecFile(project)) ?: return
        AddPackageDialogIdea(project, file).show()
    }
}