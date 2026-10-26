package shop.itbug.flutterx.widget

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import shop.itbug.flutterx.model.FlutterLocalVersion
import shop.itbug.flutterx.model.getVersionText
import shop.itbug.flutterx.tools.FlutterVersionTool
import shop.itbug.flutterx.util.launchBackgroundProgress
import java.awt.CardLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * 异步加载面板
 */
abstract class AsyncLoadingPanel<T>(val project: Project) : JPanel(CardLayout()) {

    init {
        add(createLoadingPanel(), "loading")
        SwingUtilities.invokeLater {
            startLoadTask()
        }
    }

    abstract fun loadData(): T
    abstract fun createContentPanel(data: T): JComponent

    open fun getTaskName() = "Loading"

    fun createLoadingPanel(): JComponent {
        return JBLabel("Loading...")
    }

    private fun startLoadTask() {
        project.launchBackgroundProgress(getTaskName()) {
            val data = loadData()
            withContext(Dispatchers.EDT) {
                val comp = createContentPanel(data)
                this@AsyncLoadingPanel.add(comp, "content")
                (this@AsyncLoadingPanel.layout as CardLayout).show(this@AsyncLoadingPanel, "content")
            }
        }
    }
}


///flutter 当前版本检测
class FlutterVersionCheckPanel(project: Project) : AsyncLoadingPanel<FlutterLocalVersion?>(project) {

    override fun loadData(): FlutterLocalVersion? {
        val flutterVersion = runBlocking { FlutterVersionTool.getLocalFlutterVersion(project) }
        return flutterVersion
    }

    override fun createContentPanel(data: FlutterLocalVersion?): JComponent {
        if (data == null) return JBLabel("Flutter version not found")
        val versionText = data.getVersionText()
        val button = JButton("Changelog")
        button.addActionListener {
            BrowserUtil.browse(FlutterVersionTool.buildChangeLogWebUrl(versionText))
        }
        return object : BorderLayoutPanel() {
            init {
                addToTop(JBLabel("Current Flutter version: $versionText"))
                addToCenter(button)
            }
        }
    }

    override fun getTaskName(): String {
        return "Load Flutter Version Info"
    }
}