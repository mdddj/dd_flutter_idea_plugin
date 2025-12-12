package shop.itbug.flutterx.services

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.wm.IconLikeCustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.ui.ClickListener
import com.intellij.ui.awt.RelativePoint
import shop.itbug.flutterx.actions.bar.getStatusBarActionGroup
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.MyFileUtil
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent


///用户面板
class MyUserBarFactory : StatusBarWidgetFactory {
    override fun getId(): String {
        return "user-account"
    }

    override fun getDisplayName(): String {
        return "FlutterX"
    }

    override fun isAvailable(project: Project): Boolean {
        val yamlFile = runReadAction { MyFileUtil.getPubspecFile(project) }
        return yamlFile != null
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


/**
 * 底部工具栏中的扩展操作
 */
class MyUserAccountBar(var project: Project) : TextPanel.WithIconAndArrows(), IconLikeCustomStatusBarWidget {


    private val clickListener = object : ClickListener() {
        override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
            if (!project.isDisposed) {
                showPopup()
            }
            return true
        }
    }
    override var icon: Icon?
        get() = MyIcons.dartPluginIcon
        set(_) {}

    override fun dispose() {
        clickListener.uninstall(this)
    }

    override fun ID(): String {
        return "dart plugin actions"
    }

    override fun install(statusBar: StatusBar) {
        if (project.isDisposed) {
            return
        }
        setupClickListener()
    }

    override fun getComponent(): JComponent = this


    private fun setupClickListener() {
        clickListener.installOn(this, true)
    }

    private fun showPopup() {
        val context = DataManager.getInstance().getDataContext(this)
        showActionPopup(context)
    }

    private fun showActionPopup(dataContext: DataContext, disposeCallback: Runnable? = null) {
        val component = PlatformCoreDataKeys.CONTEXT_COMPONENT.getData(dataContext)
        val popup = createPopupNew(dataContext, disposeCallback)
        val at = Point(0, -popup.content.preferredSize.height)
        if (component != null) {
            popup.show(RelativePoint(component, at))
        }
    }

    private fun createPopupNew(context: DataContext, disposeCallback: Runnable? = null): ListPopup {
        return JBPopupFactory.getInstance().createActionGroupPopup(
            PluginBundle.get("br.title"),
            getStatusBarActionGroup(),
            context,
            false,
            true,
            false,
            disposeCallback,
            10
        ) { false }
    }

}


