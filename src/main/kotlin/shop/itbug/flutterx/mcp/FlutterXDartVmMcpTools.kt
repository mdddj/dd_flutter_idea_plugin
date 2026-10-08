package shop.itbug.flutterx.mcp

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import shop.itbug.flutterx.api.vm.DartVmApp
import vm.getVm

/**
 * flutterX 对外提供的核心 MCP 工具，AI 通过这些工具发现并读取 Dart VM 内的数据。
 *
 * - [flutterx_list_apps]：先拿到 appId，后面所有工具都用它定位应用。
 * - [flutterx_vm_info]：单个应用的 VM / isolate 基本信息。
 * - [flutterx_list_data_tools]：列出都有哪些 Dart VM 数据面板可用（部分面板还带自己的 MCP 工具）。
 *
 * 扩展自己的工具请实现 [shop.itbug.flutterx.api.vm.DartVmMcpToolExtension]。
 */
class FlutterXDartVmMcpTools : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "List every Flutter app currently connected to the Dart VM service in this project. Each entry contains: appId (pass it to the other flutterx_* tools), deviceId, run mode and the VM service URL. Call this first whenever you need Flutter runtime data, and ignore apps that are no longer running."
    )
    suspend fun flutterx_list_apps(): String = guard {
        val apps = runningApps()
        mapOf(
            "count" to apps.size,
            "apps" to apps.map(DartVmApp::toMcpSummary),
            "hint" to if (apps.isEmpty()) {
                "No Flutter app is connected. Start a Flutter run configuration first."
            } else {
                "Pass one of these appId values to the other flutterx_* tools."
            }
        ).toMcpJson()
    }

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read the Dart VM details of one running Flutter app: VM version, host/target CPU, pid, start time, current memory / RSS, plus the main and system isolates. Use it to understand the runtime before digging into a specific data panel."
    )
    suspend fun flutterx_vm_info(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null
    ): String = guard {
        val app = requireApp(appId)
        val vm = app.vmService.getVm()
        mapOf(
            "app" to app.toMcpSummary(),
            "vm" to linkedMapOf(
                "name" to vm.getName(),
                "version" to vm.getVersion(),
                "pid" to vm.getPid(),
                "startTimeMilliseconds" to vm.getStartTime(),
                "embedder" to vm.getEmbedder(),
                "operatingSystem" to vm.getOperatingSystem(),
                "hostCPU" to vm.getHostCPU(),
                "targetCPU" to vm.getTargetCPU(),
                "architectureBits" to vm.getArchitectureBits(),
                "currentMemoryBytes" to vm.getCurrentMemory(),
                "currentRssBytes" to vm.getCurrentRSS(),
                "maxRssBytes" to vm.getMaxRSS(),
                "features" to vm.getFeatures()
            ),
            "isolates" to vm.getIsolates().map(::isolateSummary),
            "systemIsolates" to vm.getSystemIsolates().map(::isolateSummary)
        ).toMcpJson()
    }

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "List the Dart VM data panels registered in this IDE (Logging, Memory, Http Monitor, Riverpod, Hive, Drift ...). For each panel it reports whether a dedicated MCP toolset is available, and the tool names it exposes. Call this to discover what runtime data you can read, before falling back to flutterx_vm_info."
    )
    suspend fun flutterx_list_data_tools(): String = guard {
        val entries = DartVmMcpRegistry.catalog(currentProject())
        mapOf(
            "count" to entries.size,
            "tools" to entries,
            "hint" to "Entries with mcpTools are callable directly by name; others only have an IDE UI panel."
        ).toMcpJson()
    }

    private fun isolateSummary(isolate: vm.element.IsolateRef): Map<String, Any?> = linkedMapOf(
        "id" to isolate.getId(),
        "name" to isolate.getName(),
        "number" to isolate.getNumber(),
        "isSystemIsolate" to isolate.getIsSystemIsolate()
    )
}
