package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import vm.element.IsolateRef
import vm.getDebugPaintEnabled
import vm.getPaintBaselinesEnabled
import vm.getRepaintRainbowEnabled
import vm.getSlowAnimationsEnabled
import vm.getVm

/**
 * Vm 面板：VM 信息、isolate 列表，以及几个只读的 Flutter 调试开关。
 */
class DartVmStatusMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read the Vm tab for one running Flutter app: VM version, pid, memory, isolates, and the current debug-paint / slow-animation / baseline / repaint-rainbow flags. This does not toggle the inspector overlay. flutterx_vm_info is the shorter VM-only view."
    )
    suspend fun flutterx_vm_status(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
    ): String = guard {
        val app = requireApp(appId)
        val vm = app.vmService.getVm()
        val mainId = app.vmService.getMainIsolateId().ifBlank {
            vm.getIsolates().firstOrNull { it.getName() == "main" }?.getId().orEmpty()
        }
        val flags = if (mainId.isBlank()) {
            null
        } else {
            linkedMapOf(
                "debugPaint" to runCatching { app.vmService.getDebugPaintEnabled(mainId) }.getOrNull(),
                "slowAnimations" to runCatching { app.vmService.getSlowAnimationsEnabled(mainId) }.getOrNull(),
                "paintBaselines" to runCatching { app.vmService.getPaintBaselinesEnabled(mainId) }.getOrNull(),
                "repaintRainbow" to runCatching { app.vmService.getRepaintRainbowEnabled(mainId) }.getOrNull(),
            )
        }
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
                "features" to vm.getFeatures(),
            ),
            "mainIsolateId" to mainId.ifBlank { null },
            "debugFlags" to flags,
            "isolates" to vm.getIsolates().map(::isolateSummary),
            "systemIsolates" to vm.getSystemIsolates().map(::isolateSummary),
        ).toMcpJson()
    }

    private fun isolateSummary(isolate: IsolateRef): Map<String, Any?> = linkedMapOf(
        "id" to isolate.getId(),
        "name" to isolate.getName(),
        "number" to isolate.getNumber(),
        "isSystemIsolate" to isolate.getIsSystemIsolate(),
    )
}
