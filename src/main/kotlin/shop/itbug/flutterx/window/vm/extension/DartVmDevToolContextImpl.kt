package shop.itbug.flutterx.window.vm.extension

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import shop.itbug.flutterx.api.vm.DartVmApp
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.common.dart.FlutterXVMService
import vm.VmService

internal class DartVmDevToolContextImpl(
    override val project: Project,
    override val toolWindow: ToolWindow
) : DartVmDevToolContext {
    override fun getRunningApps(): List<DartVmApp> {
        return FlutterXVMService.getInstance(project).allFlutterApps.map(::DartVmAppImpl)
    }
}

private class DartVmAppImpl(private val app: FlutterAppInstance) : DartVmApp {
    override val appId: String get() = app.appInfo.appId
    override val vmUrl: String get() = app.appInfo.vmUrl
    override val deviceId: String get() = app.appInfo.deviceId
    override val mode: String get() = app.appInfo.mode
    override val vmService: VmService get() = app.vmService
}
