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
import vm.hive.HiveExtensionStatus
import vm.hive.HiveInspectorFrame
import vm.hive.HiveServices
import vm.hive.toHiveSearchableString
import vm.hive.toHiveSummaryText

/**
 * Hive CE 面板：列出已打开的 box，并读取其中一个 box 的条目摘要。
 */
class DartVmHiveMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read Hive CE boxes from the running Flutter app. The app must include the Hive CE devtools extension and must have opened at least one box. Omit box to list box names. Pass box to load that box and return entry summaries. Pass key together with box to return one entry. query matches entry keys. limit defaults to 50 and is capped at 200."
    )
    suspend fun flutterx_hive(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Hive box name. Omit it to only list boxes.") box: String? = null,
        @McpDescription("Exact entry key inside box. Omit it to list entries.") key: String? = null,
        @McpDescription("Case-insensitive match against entry keys. Used when box is set and key is omitted.") query: String? = null,
        @McpDescription("How many entries to return. Default 50, maximum 200.") limit: Int? = null,
    ): String = guard {
        val app = requireApp(appId)
        val project = currentProject()
        val services = HiveServices(project, app.vmService)
        try {
            services.initialize()
            val state = services.state.value
            if (state.status == HiveExtensionStatus.Unavailable) {
                mcpFail(state.errorMessage ?: "Hive CE extension is unavailable")
            }
            val errorMessage = state.errorMessage
            if (state.boxes.isEmpty() && !errorMessage.isNullOrBlank()) {
                mcpFail(errorMessage)
            }
            if (box.isNullOrBlank()) {
                return@guard mapOf(
                    "app" to app.toMcpSummary(),
                    "status" to state.status.name,
                    "count" to state.boxes.size,
                    "boxes" to state.boxes.values.map { hiveBox ->
                        linkedMapOf(
                            "name" to hiveBox.name,
                            "open" to hiveBox.open,
                        )
                    },
                    "error" to state.errorMessage,
                    "hint" to "Pass box to read its entries.",
                ).toMcpJson()
            }

            val known = state.boxes[box]
                ?: mcpFail("Unknown box '$box'. Boxes: ${state.boxes.keys.joinToString()}")
            services.loadBox(known.name)
            val loaded = services.state.value.boxes[known.name]
            val errorAfterLoad = services.state.value.errorMessage
            if (loaded == null || !loaded.loaded) {
                mcpFail(errorAfterLoad ?: "Hive box '$box' could not be loaded")
            }
            val frames = loaded.frames.values.toList()
            if (!key.isNullOrBlank()) {
                val frame = frames.firstOrNull { it.key.toString() == key }
                    ?: mcpFail("Unknown key '$key' in box '$box'")
                return@guard mapOf(
                    "app" to app.toMcpSummary(),
                    "box" to box,
                    "entry" to frame.toMcpEntry(includeFullValue = true),
                ).toMcpJson()
            }

            val token = query?.trim()?.lowercase().orEmpty()
            val matched = frames.filter { frame ->
                token.isEmpty() || frame.key.toString().lowercase().contains(token)
            }
            val count = clampCount(limit, defaultValue = 50, maxValue = 200)
            val page = matched.take(count)
            mapOf(
                "app" to app.toMcpSummary(),
                "box" to box,
                "matched" to matched.size,
                "returned" to page.size,
                "entries" to page.map { it.toMcpEntry(includeFullValue = false) },
                "error" to errorAfterLoad,
                "hint" to "Pass key to read one entry in more detail.",
            ).toMcpJson()
        } finally {
            services.dispose()
        }
    }

    private fun HiveInspectorFrame.toMcpEntry(includeFullValue: Boolean): Map<String, Any?> = linkedMapOf(
        "key" to key.toString(),
        "lazy" to lazy,
        "deleted" to deleted,
        "value" to if (includeFullValue) {
            value.toHiveSearchableString().truncateForMcp(4000)
        } else {
            value.toHiveSummaryText().truncateForMcp(500)
        },
    )
}
