package shop.itbug.flutterx.actions.bar

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SimpleListCellRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.tools.IosSimulator
import shop.itbug.flutterx.tools.IosSimulatorTool
import shop.itbug.flutterx.util.launchBackgroundProgress
import shop.itbug.flutterx.util.toastWithError
import javax.swing.JList

private val simulatorRenderer: SimpleListCellRenderer<IosSimulator> =
    object : SimpleListCellRenderer<IosSimulator>() {
        override fun customize(
            list: JList<out IosSimulator>,
            value: IosSimulator,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean,
        ) {
            text = value.toString()
            icon = MyIcons.platformIos
        }
    }

/**
 * 挑选并启动 Xcode 的 iOS 模拟器。
 *
 * 仅当 `xcrun` 存在（macOS + Xcode/Command Line Tools）且未在设置里关闭时可见。
 */
class LaunchIosSimulatorAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                IosSimulatorTool.isAvailable() &&
                PluginConfig.getState(project).showIosSimulatorLauncher
        e.presentation.text = PluginBundle.get("ios.simulator.action")
        e.presentation.description = "Launch an iOS simulator with Xcode"
        e.presentation.icon = MyIcons.platformIos
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dataContext = e.dataContext
        project.launchBackgroundProgress(PluginBundle.get("ios.simulator.loading"), cancellable = false) {
            val simulators = try {
                withContext(Dispatchers.IO) { IosSimulatorTool.listSimulators() }
            } catch (t: CancellationException) {
                throw t
            } catch (t: Throwable) {
                notifyError(project, t.message ?: PluginBundle.get("ios.simulator.load.failed"))
                return@launchBackgroundProgress
            }
            withContext(Dispatchers.EDT) {
                if (!project.isDisposed) {
                    showSimulatorPicker(project, simulators, dataContext)
                }
            }
        }
    }

    private fun showSimulatorPicker(project: Project, simulators: List<IosSimulator>, dataContext: DataContext) {
        if (simulators.isEmpty()) {
            project.toastWithError(PluginBundle.get("ios.simulator.none"))
            return
        }
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(simulators)
            .setTitle(PluginBundle.get("ios.simulator.select.title"))
            .setRenderer(simulatorRenderer)
            .setItemChosenCallback { simulator -> bootSimulator(project, simulator) }
            .createPopup()
            .showInBestPositionFor(dataContext)
    }

    private fun bootSimulator(project: Project, simulator: IosSimulator) {
        val title = PluginBundle.get("ios.simulator.launching", simulator.name)
        project.launchBackgroundProgress(title, cancellable = false) {
            val error = withContext(Dispatchers.IO) { IosSimulatorTool.boot(simulator) }
            if (error != null) {
                notifyError(project, error)
            }
        }
    }

    private suspend fun notifyError(project: Project, message: String) {
        withContext(Dispatchers.EDT) {
            if (!project.isDisposed) {
                project.toastWithError(message)
            }
        }
    }
}
