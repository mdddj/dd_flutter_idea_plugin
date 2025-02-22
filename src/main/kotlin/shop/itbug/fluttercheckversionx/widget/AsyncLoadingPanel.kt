package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.tools.FlutterVersionTool
import shop.itbug.fluttercheckversionx.util.Util
import java.awt.CardLayout
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
class FlutterVersionCheckPanel(project: Project) : AsyncLoadingPanel<String>(project) {

    override fun loadData(): String {
        val flutterChannel = Util.getFlutterChannel()
        val currentFlutterVersion = runBlocking { FlutterVersionTool.readVersionFromSdkHome(project) }
        return "flutter v${currentFlutterVersion?.version ?: "Unknown"}  channel:$flutterChannel"
    }

    override fun createContentPanel(data: String): JComponent {
        return JBLabel(data)
    }

    override fun getTaskName(): String {
        return "Load Flutter Version Info"
    }
}