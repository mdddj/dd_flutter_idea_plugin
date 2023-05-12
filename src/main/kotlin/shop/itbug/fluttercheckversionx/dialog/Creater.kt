package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import javax.swing.JComponent

///创建一个dialog并显示
fun Project.createDialogAndShow(title: String, ui: () -> JComponent) : Boolean {
    return createDialog(project = this, title = title, ui)
}

fun createDialog(project: Project, title: String, ui: () -> JComponent) : Boolean {
    val dialog = object : MyDialogWrapper(project) {
        init {
            super.init()
            this.title = title
        }

        override fun createCenterPanel(): JComponent {
            return ui()
        }


    }
   return dialog.showAndGet()
}