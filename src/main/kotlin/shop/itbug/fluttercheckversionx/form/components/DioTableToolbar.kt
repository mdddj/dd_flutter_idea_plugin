package shop.itbug.fluttercheckversionx.form.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*

///表格的操作工具栏
class DioTableToolbar {
    companion object {
        fun create(clean: () -> Unit,projectfilter: SelectProject): ActionToolbar {
            val appbar = ActionManager.getInstance().createActionToolbar(
                "jtable-bar",
                MyActionGroups(clean,projectfilter),
                true
            )
            return appbar
        }
    }
}
internal class MyActionGroups(var clean: ()->Unit, private val projectSelect: SelectProject?) : DefaultActionGroup() {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val mutableListOf = mutableListOf<AnAction>()
        val removeAction: AnAction = object : AnAction("清空") {
            override fun actionPerformed(anActionEvent: AnActionEvent) {
                clean()
            }
        }
        removeAction.templatePresentation.icon = AllIcons.Actions.GC
        mutableListOf.add(removeAction)
        if(projectSelect!=null){
            val projectFilter = ProjectFilter(projectSelect)
            mutableListOf.add(projectFilter)

        }
        return mutableListOf.toList().toTypedArray()
    }
}