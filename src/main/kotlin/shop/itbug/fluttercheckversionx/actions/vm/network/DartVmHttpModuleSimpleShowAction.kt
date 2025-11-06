package shop.itbug.fluttercheckversionx.actions.vm.network

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInstance
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.model.toCurlStringAsDartDevTools
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.window.vm.hasMeaningfulPathWithOkHttp
import vm.network.NetworkRequest
import java.awt.event.MouseEvent
import kotlin.time.Duration.Companion.microseconds

/**
 * 展示http请求,简单版本,
 *
 * 弹出一个 popup action group,提供当前运行 flutter app的列表
 * 用户选择 app,右侧再次展开一个 popup action group,展示监听到 http请求列表
 *
 * action只有在存在运行中的 app时才显示
 */
class DartVmHttpModuleSimpleShowAction : AnAction(), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val component = e.inputEvent?.component ?: return

        val actionGroup = FlutterAppsActionGroup(project)
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Select Flutter App",
            actionGroup,
            e.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )

        if (e.inputEvent is MouseEvent) {
            popup.show(RelativePoint(e.inputEvent as MouseEvent))
        } else {
            popup.showInBestPositionFor(e.dataContext)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val vmService = FlutterXVMService.getInstance(project)
        val hasRunningApps = vmService.runningApps.value.isNotEmpty()
        e.presentation.isPopupGroup = true
        e.presentation.isEnabledAndVisible = hasRunningApps
        e.presentation.text = "HTTP Requests"
        e.presentation.description = "View HTTP requests from running Flutter apps"
        e.presentation.icon = AllIcons.Debugger.ThreadRunning
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * Flutter App 列表的 ActionGroup
 */
private class FlutterAppsActionGroup(private val project: Project) : DefaultActionGroup(), DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val vmService = FlutterXVMService.getInstance(project)
        val runningApps = vmService.runningApps.value

        return runningApps.map { app ->
            FlutterAppAction(project, app)
        }.toTypedArray()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * 单个 Flutter App 的 Action，点击后展开 HTTP 请求列表
 */
private class FlutterAppAction(
    private val project: Project,
    private val app: FlutterAppInstance
) : AnAction(app.appInfo.deviceId, "View HTTP requests for ${app.appInfo.deviceId}", AllIcons.Nodes.Module), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val actionGroup = HttpRequestsActionGroup(project, app)
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "HTTP Requests - ${app.appInfo.deviceId}",
            actionGroup,
            e.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )

        if (e.inputEvent is MouseEvent) {
            popup.show(RelativePoint(e.inputEvent as MouseEvent))
        } else {
            popup.showInBestPositionFor(e.dataContext)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * HTTP 请求列表的 ActionGroup
 */
private class HttpRequestsActionGroup(
    private val project: Project,
    private val app: FlutterAppInstance
) : DefaultActionGroup(), DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val requests = app.vmService.dartHttpMonitor.requests.value.values.filter {
            it.isLikelyImage.not() && hasMeaningfulPathWithOkHttp(it.requestUrl)
        }

            .toList()

        if (requests.isEmpty()) {
            return arrayOf(
                object : AnAction("No HTTP requests captured") {
                    override fun actionPerformed(e: AnActionEvent) {}
                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabled = false
                    }

                    override fun getActionUpdateThread() = ActionUpdateThread.BGT
                }
            )
        }

        val actions = mutableListOf<AnAction>()

        // 添加控制按钮
        actions.add(Separator.create("Controls"))
        actions.add(StartMonitoringAction(app))
        actions.add(StopMonitoringAction(app))
        actions.add(ClearRequestsAction(app))

        // 添加请求列表
        actions.add(Separator.create("Requests (${requests.size})"))

        requests.reversed().forEach { request ->
            actions.add(HttpRequestAction(project, request))
        }

        return actions.toTypedArray()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * 单个 HTTP 请求的 Action
 */
private class HttpRequestAction(
    private val project: Project,
    private val request: NetworkRequest
) : AnAction(), DumbAware {

    init {
        templatePresentation.text = buildRequestText()
        templatePresentation.description = request.uri
        templatePresentation.icon = getStatusIcon()
    }

    private fun buildRequestText(): String {
        val method = request.method.padEnd(6)
        val status = request.statusCode?.toString() ?: "..."
        val duration = request.duration?.let { "${it.microseconds.inWholeMilliseconds}ms" } ?: "..."
        val url = request.uri.take(60) + if (request.uri.length > 60) "..." else ""
        return "$method [$status] $duration - $url"
    }

    private fun getStatusIcon() = when {
        request.statusCode == null -> AllIcons.Process.Step_1
        request.statusCode in 200..299 -> AllIcons.RunConfigurations.TestPassed
        request.statusCode in 400..599 -> AllIcons.RunConfigurations.TestError
        else -> AllIcons.Process.Step_2
    }

    override fun actionPerformed(e: AnActionEvent) {
        val actionGroup = RequestDetailsActionGroup(project, request)
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Request Details",
            actionGroup,
            e.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )

        if (e.inputEvent is MouseEvent) {
            popup.show(RelativePoint(e.inputEvent as MouseEvent))
        } else {
            popup.showInBestPositionFor(e.dataContext)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

/**
 * 请求详情操作的 ActionGroup
 */
private class RequestDetailsActionGroup(
    private val project: Project,
    private val request: NetworkRequest
) : DefaultActionGroup(), DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            CopyUrlAction(request),
            CopyCurlAction(request),
            ViewResponseAction(project, request),
            ViewRequestBodyAction(project, request),
            Separator.create(),
            ViewRawDataAction(project, request)
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}

// 控制操作
private class StartMonitoringAction(private val app: FlutterAppInstance) : AnAction(
    "Start Monitoring",
    "Start monitoring HTTP requests",
    AllIcons.Actions.Execute
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        app.vmService.runInScope {
            app.vmService.dartHttpMonitor.startMonitoring()
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !app.vmService.dartHttpMonitor.isMonitoring.value
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class StopMonitoringAction(private val app: FlutterAppInstance) : AnAction(
    "Stop Monitoring",
    "Stop monitoring HTTP requests",
    AllIcons.Actions.Suspend
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        app.vmService.dartHttpMonitor.stop()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = app.vmService.dartHttpMonitor.isMonitoring.value
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class ClearRequestsAction(private val app: FlutterAppInstance) : AnAction(
    "Clear All Requests",
    "Clear all captured HTTP requests",
    AllIcons.Actions.GC
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        app.vmService.runInScope {
            app.vmService.dartHttpMonitor.clearRequests()
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

// 请求详情操作
private class CopyUrlAction(private val request: NetworkRequest) : AnAction(
    "Copy URL",
    "Copy request URL to clipboard",
    AllIcons.Actions.Copy
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        request.uri.copyTextToClipboard()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class CopyCurlAction(private val request: NetworkRequest) : AnAction(
    "Copy as cURL",
    "Copy request as cURL command",
    AllIcons.Actions.Copy
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        request.toCurlStringAsDartDevTools().copyTextToClipboard()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class ViewResponseAction(
    private val project: Project,
    private val request: NetworkRequest
) : AnAction(
    "View Response Body",
    "Open response body in editor",
    AllIcons.Actions.Preview
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val body = request.responseBody ?: "No response body"
        MyFileUtil.showJsonInEditor(project, body)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = request.responseBody != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class ViewRequestBodyAction(
    private val project: Project,
    private val request: NetworkRequest
) : AnAction(
    "View Request Body",
    "Open request body in editor",
    AllIcons.Actions.Preview
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val body = request.requestBody ?: "No request body"
        MyFileUtil.showJsonInEditor(project, body)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = request.requestBody != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}

private class ViewRawDataAction(
    private val project: Project,
    private val request: NetworkRequest
) : AnAction(
    "View Raw Data",
    "View complete request data",
    AllIcons.FileTypes.Json
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        MyFileUtil.showJsonInEditor(project, gson.toJson(request))
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}