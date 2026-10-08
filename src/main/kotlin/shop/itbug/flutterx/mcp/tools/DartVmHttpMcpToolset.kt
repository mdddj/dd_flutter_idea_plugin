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
import vm.network.NetworkRequest

/**
 * Http Monitor 面板：读取 Dart HTTP timeline 里已经抓到的请求。
 */
class DartVmHttpMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read HTTP requests captured by the Http Monitor tab. The first call starts Dart HTTP timeline logging, the same way opening the tab does, and only requests after that are visible. Omit requestId for a list without bodies. Pass requestId to include truncated request and response bodies; image bodies are omitted. query matches method or URI. limit defaults to 30 and is capped at 100."
    )
    suspend fun flutterx_http(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("HTTP request id from a previous flutterx_http list. Omit it to list requests without bodies.") requestId: String? = null,
        @McpDescription("Case-insensitive match against HTTP method or URI.") query: String? = null,
        @McpDescription("How many requests to return. Default 30, maximum 100. Ignored when requestId is set.") limit: Int? = null,
    ): String = guard {
        val app = requireApp(appId)
        val monitor = app.vmService.dartHttpMonitor
        val started = monitor.startMonitoring()
        if (!started && monitor.requests.value.isEmpty()) {
            mcpFail(monitor.statusMessage.value.ifBlank { "HTTP profiling is unavailable for this app" })
        }
        monitor.updateRequests()
        val token = query?.trim()?.lowercase().orEmpty()
        val matched = monitor.requests.value.values
            .filter { request ->
                token.isEmpty() ||
                        request.method.lowercase().contains(token) ||
                        request.uri.lowercase().contains(token)
            }
            .sortedByDescending { it.startTime }

        if (!requestId.isNullOrBlank()) {
            val existing = matched.firstOrNull { it.id == requestId }
                ?: monitor.requests.value[requestId]
                ?: mcpFail("Unknown requestId '$requestId'. Call flutterx_http without requestId to list ids.")
            val detailed = monitor.getRequestDetails(requestId) ?: existing
            return@guard mapOf(
                "app" to app.toMcpSummary(),
                "monitoring" to monitor.isMonitoring.value,
                "request" to detailed.toMcpSummary(includeBodies = true),
            ).toMcpJson()
        }

        val count = clampCount(limit, defaultValue = 30, maxValue = 100)
        val page = matched.take(count)
        mapOf(
            "app" to app.toMcpSummary(),
            "monitoring" to monitor.isMonitoring.value,
            "status" to monitor.statusMessage.value,
            "matched" to matched.size,
            "returned" to page.size,
            "requests" to page.map { it.toMcpSummary(includeBodies = false) },
            "hint" to "Pass requestId to read truncated request and response bodies.",
        ).toMcpJson()
    }

    private fun NetworkRequest.toMcpSummary(includeBodies: Boolean): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "method" to method,
        "uri" to uri.truncateForMcp(500),
        "status" to status.name,
        "statusCode" to statusCode,
        "startTime" to startTime,
        "durationMs" to duration,
        "contentLength" to contentLength,
        "error" to error,
        "requestBody" to if (includeBodies) requestBody?.truncateForMcp(4000) else null,
        "responseBody" to if (includeBodies && !isLikelyImage) responseBody?.truncateForMcp(4000) else null,
        "responseBodyOmitted" to if (includeBodies && isLikelyImage) "image" else null,
    )
}
