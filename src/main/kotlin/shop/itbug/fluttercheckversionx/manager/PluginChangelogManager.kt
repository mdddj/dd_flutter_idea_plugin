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


class PluginChangelogState : BaseState() {
    var versions by stringSet()

    fun add(version: String) {
        versions.add(version)
        incrementModificationCount()
    }

    fun has(version: String): Boolean {
        return this.versions.contains(version)
    }

}

@Service
@State(name = "FlutterXChangelog", storages = [Storage("FlutterXChangelogVersions.xml")])
class PluginChangelogCache : SimplePersistentStateComponent<PluginChangelogState>(PluginChangelogState()) {

    val version: String get() = FlutterXPluginInfo.Version
    val changelog: String get() = FlutterXPluginInfo.Changelog

    // 开始显示更新日志
    fun startShow(project: Project) {
        if (!state.has(version)) {
            val n = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
                changelog, null, JBColor.foreground(),
                JBColor.PanelBackground, null
            )
                .setCloseButtonEnabled(true)
                .setTitle("FlutterX Changelog")
                .createBalloon()

            // 在右上角显示
            val component = WindowManager.getInstance().getIdeFrame(project)?.component
            if (component != null) {
                val point = Point(component.width, 0)
                val relativePoint = RelativePoint(component, point)
                n.show(relativePoint, Balloon.Position.atLeft)
            }
            showed()
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