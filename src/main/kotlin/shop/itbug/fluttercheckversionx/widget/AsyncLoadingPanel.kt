package shop.itbug.fluttercheckversionx.widget

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.model.FlutterLocalVersion
import shop.itbug.fluttercheckversionx.model.getVersionText
import shop.itbug.fluttercheckversionx.tools.FlutterVersionTool
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
        val task = object : Task.Backgroundable(project, getTaskName()) {
            override fun run(indicator: ProgressIndicator) {
                val data = loadData()
                val comp = createContentPanel(data)
                SwingUtilities.invokeLater {
                    this@AsyncLoadingPanel.add(comp, "content")
                    (this@AsyncLoadingPanel.layout as CardLayout).show(this@AsyncLoadingPanel, "content")
                }

            }
        }
        task.queue()
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