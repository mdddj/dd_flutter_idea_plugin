package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.mcpserver.mcpFail
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import shop.itbug.flutterx.mcp.truncateForMcp
import vm.devtool.RiverpodHelper
import vm.devtool.RiverpodProviderInfo
import vm.devtool.RiverpodStateRenderer

/**
 * Riverpod 面板：读取最近若干帧里出现过的 provider，以及其中一个的当前 state。
 */
class DartVmRiverpodMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read Riverpod providers from RiverpodDevtool in a debug Flutter app. Only the latest frames are scanned, so providers that did not change inside that window are omitted; raise frameLimit to look further back. Omit elementId to list providers. Pass elementId to also render that provider's current state text. query matches name, arg, or elementId. limit defaults to 50 and is capped at 200. frameLimit defaults to 20 and is capped at 100."
    )
    suspend fun flutterx_riverpod(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Provider elementId from a previous flutterx_riverpod list. Omit it to only list providers.") elementId: String? = null,
        @McpDescription("Case-insensitive match against provider name, arg, or elementId.") query: String? = null,
        @McpDescription("How many providers to return. Default 50, maximum 200.") limit: Int? = null,
        @McpDescription("How many of the newest Riverpod frames to scan. Default 20, maximum 100.") frameLimit: Int? = null,
    ): String = guard {
        val app = requireApp(appId)
        val eval = RiverpodHelper.getRiverpodEval(app.vmService)
            ?: mcpFail("Riverpod package is not loaded in this app")
        if (!RiverpodHelper.checkAvailable(eval, app.vmService)) {
            mcpFail("RiverpodDevtool is not available. Run the app in debug mode with riverpod_devtool enabled.")
        }
        val frameCount = RiverpodHelper.getFrameCount(eval, app.vmService)
        val framesToScan = clampCount(frameLimit, defaultValue = 20, maxValue = 100)
        val start = (frameCount - framesToScan).coerceAtLeast(0)
        val frames = RiverpodHelper.getNewFrames(eval, app.vmService, start)
        val token = query?.trim()?.lowercase().orEmpty()
        val providers = RiverpodHelper.extractProviders(frames)
            .filter { provider ->
                token.isEmpty() ||
                        provider.name.lowercase().contains(token) ||
                        provider.arg.lowercase().contains(token) ||
                        provider.elementId.lowercase().contains(token)
            }
            .sortedWith(compareBy<RiverpodProviderInfo> { it.name.lowercase() }.thenBy { it.arg })

        if (!elementId.isNullOrBlank()) {
            val provider = providers.firstOrNull { it.elementId == elementId }
                ?: RiverpodHelper.extractProviders(frames).firstOrNull { it.elementId == elementId }
                ?: mcpFail(
                    "Unknown elementId '$elementId' in the latest $framesToScan frames. " +
                            "Raise frameLimit or call flutterx_riverpod without elementId."
                )
            val current = RiverpodHelper.accumulateElements(frames).second.lastOrNull().orEmpty()
            val state = RiverpodHelper.getStateInstance(
                eval,
                app.vmService,
                current[elementId]?.statePath,
            )
            val rendered = RiverpodStateRenderer.render(eval, app.vmService, state).truncateForMcp(4000)
            return@guard mapOf(
                "app" to app.toMcpSummary(),
                "frameCount" to frameCount,
                "framesScanned" to frames.size,
                "provider" to provider.toMcpSummary(),
                "state" to rendered.ifBlank { null },
            ).toMcpJson()
        }

        val count = clampCount(limit, defaultValue = 50, maxValue = 200)
        val page = providers.take(count)
        mapOf(
            "app" to app.toMcpSummary(),
            "frameCount" to frameCount,
            "framesScanned" to frames.size,
            "matched" to providers.size,
            "returned" to page.size,
            "providers" to page.map { it.toMcpSummary() },
            "hint" to "Pass elementId to render one provider state. Raise frameLimit if a provider is missing.",
        ).toMcpJson()
    }

    private fun RiverpodProviderInfo.toMcpSummary(): Map<String, Any?> = linkedMapOf(
        "elementId" to elementId,
        "name" to name,
        "arg" to arg,
        "status" to status?.name,
        "hasState" to hasState,
        "frameIndex" to frameIndex,
        "containerId" to containerId,
    )
}
