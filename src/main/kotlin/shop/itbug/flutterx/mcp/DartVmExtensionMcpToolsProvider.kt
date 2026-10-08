package shop.itbug.flutterx.mcp

import com.intellij.mcpserver.McpTool
import com.intellij.mcpserver.McpToolsProvider
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.impl.util.asTools
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import shop.itbug.flutterx.api.vm.DartVmDevToolExtension
import shop.itbug.flutterx.api.vm.DartVmDevToolExtensionBean
import shop.itbug.flutterx.api.vm.DartVmMcpToolExtension

/**
 * 收集所有实现 [DartVmMcpToolExtension] 的 Dart VM Tab，把它们的 [McpToolset]
 * 变成真正的 MCP 工具注册给 IDE 的 MCP server。
 *
 * 注册方式见 `plugin.xml` 里的 `mcpServer.mcpToolsProvider` 扩展点。
 */
class DartVmExtensionMcpToolsProvider : McpToolsProvider {
    override fun getTools(): List<McpTool> = DartVmMcpRegistry.extensionTools()
}

/**
 * Dart VM 扩展的 MCP 工具索引。扩展实例和工具集只创建一次并缓存，
 * 因此工具集实现必须保持无状态。
 */
object DartVmMcpRegistry {

    private val logger = thisLogger()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    private data class Entry(
        val id: String,
        val extension: DartVmDevToolExtension,
        val toolset: McpToolset?,
        val tools: List<McpTool>,
    )

    private val entries: List<Entry> by lazy { collectEntries() }

    /** 所有已注册的 MCP 工具（来自扩展），供 [DartVmExtensionMcpToolsProvider] 使用。 */
    fun extensionTools(): List<McpTool> = entries.flatMap(Entry::tools)

    /** 面板目录，供 `flutterx_list_data_tools` 使用。 */
    fun catalog(project: Project): List<Map<String, Any?>> = entries.map { entry ->
        linkedMapOf(
            "id" to entry.id,
            "tabTitle" to runCatching { entry.extension.getTabTitle(project) }.getOrDefault(entry.id),
            "hasMcpTools" to entry.tools.isNotEmpty(),
            "mcpTools" to entry.tools.map { it.descriptor.name },
        )
    }

    private fun collectEntries(): List<Entry> {
        val result = mutableListOf<Entry>()
        DartVmDevToolExtensionBean.EP_NAME.extensionList.forEach { bean ->
            val extension = runCatching { bean.createExtension() }
                .onFailure { logger.warn("Cannot instantiate Dart VM dev tool ${bean.id}", it) }
                .getOrNull() ?: return@forEach
            val toolset = (extension as? DartVmMcpToolExtension)?.let { mcp ->
                runCatching { mcp.createMcpToolset() }
                    .onFailure { logger.warn("Cannot create MCP toolset for Dart VM dev tool ${bean.id}", it) }
                    .getOrNull()
            }
            result += Entry(bean.id, extension, toolset, toolset.toMcpTools(bean.id))
        }
        if (result.any { it.tools.isNotEmpty() }) {
            logger.info("FlutterX MCP tools: " + result.flatMap(Entry::tools).joinToString { it.descriptor.name })
        }
        return result
    }

    private fun McpToolset?.toMcpTools(toolId: String): List<McpTool> {
        if (this == null) return emptyList()
        return runCatching { asTools(json) }
            .onFailure { logger.warn("Cannot reflect MCP tools of Dart VM dev tool $toolId", it) }
            .getOrDefault(emptyList())
    }
}
