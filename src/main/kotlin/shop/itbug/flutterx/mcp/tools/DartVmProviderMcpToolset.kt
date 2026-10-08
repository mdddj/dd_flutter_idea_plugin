package shop.itbug.flutterx.mcp.tools

import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.progress.ProcessCanceledException
import shop.itbug.flutterx.mcp.DartVmMcpToolset
import shop.itbug.flutterx.mcp.toMcpJson
import shop.itbug.flutterx.mcp.toMcpSummary
import shop.itbug.flutterx.mcp.truncateForMcp
import vm.devtool.InstanceDetails
import vm.devtool.ProviderHelper
import vm.devtool.ProviderNode
import kotlin.coroutines.cancellation.CancellationException

/**
 * Provider 面板：列出 ProviderBinding 里的 provider，并读取其中一个的当前值。
 */
class DartVmProviderMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription(
        "Read Flutter Provider nodes from ProviderBinding.debugInstance. The app must be in debug mode and must have created at least one Provider. Omit providerId to list id and type. Pass providerId to also read that provider's current value: primitives are returned in full, objects return their type and field names only. query matches id or type. limit defaults to 100 and is capped at 300."
    )
    suspend fun flutterx_providers(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
        @McpDescription("Provider id from a previous flutterx_providers list. Omit it to only list providers.") providerId: String? = null,
        @McpDescription("Case-insensitive match against provider id or type.") query: String? = null,
        @McpDescription("How many providers to return when providerId is omitted. Default 100, maximum 300.") limit: Int? = null,
    ): String = guard {
        val app = requireApp(appId)
        val nodes = try {
            ProviderHelper.getProviderNodes(app.vmService)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            mcpFail("Cannot read Provider nodes: ${e.message ?: e.javaClass.simpleName}")
        }
        val token = query?.trim()?.lowercase().orEmpty()
        val matched = nodes.filter { node ->
            token.isEmpty() ||
                    node.id.lowercase().contains(token) ||
                    node.type.lowercase().contains(token)
        }.sortedBy { it.type.lowercase() }

        if (!providerId.isNullOrBlank()) {
            val node = nodes.firstOrNull { it.id == providerId }
                ?: mcpFail("Unknown providerId '$providerId'. Known ids: ${nodes.joinToString { it.id }}")
            val details = ProviderHelper.getInstanceDetails(app.vmService, node.getProviderPath())
            return@guard mapOf(
                "app" to app.toMcpSummary(),
                "provider" to node.toMcpSummary(),
                "value" to details.toMcpValue(),
            ).toMcpJson()
        }

        val count = clampCount(limit, defaultValue = 100, maxValue = 300)
        val page = matched.take(count)
        mapOf(
            "app" to app.toMcpSummary(),
            "matched" to matched.size,
            "returned" to page.size,
            "providers" to page.map { it.toMcpSummary() },
            "hint" to "Pass providerId to read one provider value.",
        ).toMcpJson()
    }

    private fun ProviderNode.toMcpSummary(): Map<String, Any?> = linkedMapOf(
        "id" to id,
        "type" to type,
    )

    private fun InstanceDetails.toMcpValue(): Map<String, Any?> = when (this) {
        InstanceDetails.Nill -> mapOf("kind" to "null")
        is InstanceDetails.Bool -> mapOf("kind" to "bool", "value" to displayString)
        is InstanceDetails.Number -> mapOf("kind" to "number", "value" to displayString)
        is InstanceDetails.DartString -> mapOf("kind" to "string", "value" to displayString.truncateForMcp(2000))
        is InstanceDetails.Enum -> mapOf("kind" to "enum", "type" to type, "value" to value)
        is InstanceDetails.DartList -> mapOf("kind" to "list", "length" to length)
        is InstanceDetails.Map -> mapOf("kind" to "map", "size" to associations.size)
        is InstanceDetails.Object -> mapOf(
            "kind" to "object",
            "type" to type,
            "fields" to fieldsFiltered.take(40).map { it.name },
        )
    }
}
