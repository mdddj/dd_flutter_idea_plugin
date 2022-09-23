package shop.itbug.fluttercheckversionx.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import shop.itbug.fluttercheckversionx.dialog.SearchDialog
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.PluginActions.*
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent


enum class PluginActions(val title: String) {
    SearchPlugin("搜索pub包"),CheckVersion("检测版本更新")
}

///用户面板
class MyUserBarFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "user-account"
    }

    override fun getDisplayName(): String {
        return "典典账号登录"
    }

    override fun isAvailable(project: Project): Boolean {
        return true
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return MyUserAccountBar(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
        return true
    }
}


///底部状态栏的组件
class MyUserAccountBar(var project: Project): CustomStatusBarWidget {

    val icon = MyIcons.dartPluginIcon
    val iconLabel = JBLabel(icon)


    override fun dispose() {
    }

    override fun ID(): String {
        return "dart plugin actions"
    }

    override fun install(statusBar: StatusBar) {
    }

    override fun getComponent(): JComponent {


        iconLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                e?.let {
                    showPop()
                }
                super.mouseClicked(e)
            }
        })
        return iconLabel
    }



    fun showPop(){
        val pop = createPop()
        val h = pop.content.preferredSize.height
        val w = pop.content.preferredSize.width
        pop.show(RelativePoint(Point(iconLabel.locationOnScreen.x-w+iconLabel.preferredSize.width,iconLabel.locationOnScreen.y-h)))
    }

    private fun createPop():JBPopup {
       return JBPopupFactory.getInstance().createPopupChooserBuilder(values().asList())
            .setItemChosenCallback {
                when(it){
                    SearchPlugin -> {
                        SearchDialog(project).show()
                    }
                    CheckVersion -> {

                    }
                }
            }
           .setRenderer { list, value, index, isSelected, cellHasFocus ->
               return@setRenderer JBLabel(value.title)
           }
           .setTitle("Flutter工具12")
            .createPopup()
    }


}