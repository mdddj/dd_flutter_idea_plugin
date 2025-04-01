package shop.itbug.fluttercheckversionx.manager

import codegen.FlutterXPluginInfo
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.Point


///
class PluginChangelogState : BaseState() {
    private var versions by stringSet()

    fun add(version: String) {
        versions.add(version)
        incrementModificationCount()
    }

    fun has(version: String): Boolean {
        return this.versions.contains(version)
    }

}

@Service(Service.Level.APP)
@State(name = "FlutterXChangelog", storages = [Storage("FlutterXChangelogVersions.xml")])
class PluginChangelogCache : SimplePersistentStateComponent<PluginChangelogState>(PluginChangelogState()) {

    val version: String get() = FlutterXPluginInfo.Version
    private val changelog: String get() = FlutterXPluginInfo.Changelog
    private val balloon
        get() = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
            changelog, null, JBColor.foreground(),
            JBColor.PanelBackground, null
        )
            .setCloseButtonEnabled(true)
            .setTitle("FlutterX Changelog")
            .createBalloon()

    // 开始显示更新日志
    fun startShow(project: Project) {
        println("开始显示版本更新记录")
        if (!state.has(version)) {

            // 在右上角显示
            val component = WindowManager.getInstance().getIdeFrame(project)?.component
            if (component != null) {
                val point = Point(component.width, 0)
                val relativePoint = RelativePoint(component, point)
                balloon.show(relativePoint, Balloon.Position.atLeft)

            }
            showed()
        }
    }

    fun testShow(project: Project) {

        // 在右上角显示
        val component = WindowManager.getInstance().getIdeFrame(project)?.component
        if (component != null) {
            val point = Point(component.width, 0)
            val relativePoint = RelativePoint(component, point)
            balloon.show(relativePoint, Balloon.Position.atLeft)
        }
    }


    // 当前版本已显示
    private fun showed() {
        state.add(version)
    }

    companion object {
        fun getInstance() = service<PluginChangelogCache>()
    }
}