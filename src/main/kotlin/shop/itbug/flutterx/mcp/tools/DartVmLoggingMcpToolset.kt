package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import shop.itbug.flutterx.mcp.truncateForMcp
import vm.log.LogData

/**
 * Logging 面板：读取已经缓存在 IDE 里的 Flutter / Dart 日志。
 */
class DartVmLoggingMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read recent Flutter and Dart logs captured by the Logging tab: stdout, stderr, Flutter errors, navigation, and developer logs. GC logs are hidden unless includeGc is true. The buffer starts when the Logging tab or this tool is first used, so an empty result means nothing has been captured yet; call again after the app prints something. Results are newest first. limit defaults to 50 and is capped at 200."
    )
    suspend fun flutterx_logs(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Case-insensitive match against kind, summary, or already-loaded details. Omit it to return the newest logs.") query: String? = null,
        @McpDescription("How many matching logs to return. Default 50, maximum 200.") limit: Int? = null,
        @McpDescription("Include Dart GC logs. Default false.") includeGc: Boolean = false,
    ): String = guard {
        val app = requireApp(appId)
        val count = clampCount(limit, defaultValue = 50, maxValue = 200)
        val controller = app.vmService.logController
        val logs = controller.snapshot(count, query, includeGc)
        mapOf(
            "app" to app.toMcpSummary(),
            "buffered" to controller.bufferedLogCount(),
            "returned" to logs.size,
            "includeGc" to includeGc,
            "query" to query,
            "logs" to logs.map(::logSummary),
            "hint" to if (logs.isEmpty()) {
                "No matching logs are buffered yet. Logging starts when this tool or the Logging tab is used."
            } else {
                null
            },
        ).toMcpJson()
    }

    private fun logSummary(log: LogData): Map<String, Any?> = linkedMapOf(
        "timestamp" to log.timestamp,
        "time" to log.formattedTimestamp,
        "kind" to log.kind,
        "level" to log.levelName,
        "isError" to log.isError,
        "isolate" to log.isolateRef?.getName(),
        "summary" to log.summary?.truncateForMcp(500),
        "details" to log.details.value?.truncateForMcp(1500),
    )
}
