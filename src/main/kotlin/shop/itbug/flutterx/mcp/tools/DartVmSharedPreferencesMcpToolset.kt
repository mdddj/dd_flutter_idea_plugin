package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.mcpserver.mcpFail
import shop.itbug.flutterx.api.vm.DartVmMcpToolExtension
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import vm.sp.AsyncState
import vm.sp.SharedPreferencesServices

/**
 * 示例：Shared Preferences 面板的 MCP 工具。
 *
 * 新增一个面板的 MCP 工具只需要三步：
 * 1. 继承 [DartVmMcpToolset]，写 `@McpTool` 方法；
 * 2. 让 XxxDevToolExtension 额外实现 [DartVmMcpToolExtension] 并在 `createMcpToolset()` 里返回它；
 * 3. 不需要改 plugin.xml —— 扩展点会自动收集。
 */
class DartVmSharedPreferencesMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read Flutter SharedPreferences keys, and optionally the value of one key. The app must use shared_preferences >= 2.3.0 and must have called SharedPreferences.getInstance() at least once, otherwise the Dart side devtools extension is not loaded and this tool fails. Omit `key` to list all keys only."
    )
    suspend fun flutterx_shared_preferences(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Exact SharedPreferences key to read. Omit it to only list every key.") key: String? = null,
    ): String = guard {
        val app = requireApp(appId)
        val services = SharedPreferencesServices(app.vmService)
        try {
            services.fetchAllKeys()
            val keys = when (val state = services.state.value.allKeys) {
                is AsyncState.Data -> state.data
                is AsyncState.Error -> mcpFail("Cannot read SharedPreferences keys: ${state.error.message}")
                AsyncState.Loading -> mcpFail("SharedPreferences keys request did not finish")
            }

            var value: Any? = null
            if (!key.isNullOrBlank()) {
                services.selectKey(key)
                value = when (val state = services.state.value.selectedKey?.value) {
                    is AsyncState.Data -> mapOf("kind" to state.data.kind, "value" to state.data.value)
                    is AsyncState.Error -> mcpFail("Cannot read value of '$key': ${state.error.message}")
                    else -> mcpFail("Value of '$key' is not available")
                }
            }

            mapOf(
                "app" to app.toMcpSummary(),
                "count" to keys.size,
                "keys" to keys,
                "key" to key,
                "value" to value,
            ).toMcpJson()
        } finally {
            services.dispose()
        }
    }
}
