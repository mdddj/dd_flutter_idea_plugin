package shop.itbug.fluttercheckversionx.actions.jobs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import shop.itbug.fluttercheckversionx.dialog.jobs.AddJobsDialog

class WriteJobPostAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let{
            AddJobsDialog(it).show()
        }
    }
}