import com.intellij.mcpserver.impl.util.asTools
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.serialization.json.Json
import shop.itbug.flutterx.mcp.FlutterXDartVmMcpTools
import shop.itbug.flutterx.mcp.tools.DartVmDriftMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmHiveMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmHttpMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmLoggingMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmMemoryMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmProviderMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmRiverpodMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmSharedPreferencesMcpToolset
import shop.itbug.flutterx.mcp.tools.DartVmStatusMcpToolset

/**
 * 校验 `@McpTool` 注解能被转换成 IDE 的 MCP 工具描述，避免出现「工具静默不注册」。
 */
class McpToolsetReflectionTest : BasePlatformTestCase() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun testCoreToolsAreDiscovered() {
        val tools = FlutterXDartVmMcpTools().asTools(json)
        val names = tools.map { it.descriptor.name }.sorted()
        assertEquals(
            listOf("flutterx_list_apps", "flutterx_list_data_tools", "flutterx_vm_info"),
            names
        )
        val vmInfo = tools.first { it.descriptor.name == "flutterx_vm_info" }
        assertTrue(vmInfo.descriptor.description!!.contains("Dart VM"))
        // appId 带默认值，所以不是必填项
        assertEquals(emptySet<String>(), vmInfo.descriptor.inputSchema.requiredProperties)
        assertTrue(vmInfo.descriptor.inputSchema.propertiesSchema.containsKey("appId"))
    }

    fun testExtensionToolsAreDiscovered() {
        val tools = DartVmSharedPreferencesMcpToolset().asTools(json)
        assertEquals(listOf("flutterx_shared_preferences"), tools.map { it.descriptor.name })
        val tool = tools.first()
        assertEquals(emptySet<String>(), tool.descriptor.inputSchema.requiredProperties)
        assertTrue(tool.descriptor.inputSchema.propertiesSchema.containsKey("appId"))
        assertTrue(tool.descriptor.inputSchema.propertiesSchema.containsKey("key"))
    }

    fun testPanelToolsAreDiscovered() {
        val cases = listOf(
            DartVmStatusMcpToolset() to "flutterx_vm_status",
            DartVmMemoryMcpToolset() to "flutterx_memory",
            DartVmHttpMcpToolset() to "flutterx_http",
            DartVmLoggingMcpToolset() to "flutterx_logs",
            DartVmProviderMcpToolset() to "flutterx_providers",
            DartVmRiverpodMcpToolset() to "flutterx_riverpod",
            DartVmHiveMcpToolset() to "flutterx_hive",
            DartVmDriftMcpToolset() to "flutterx_drift",
        )
        cases.forEach { (toolset, name) ->
            val tools = toolset.asTools(json)
            assertEquals(toolset::class.java.simpleName, listOf(name), tools.map { it.descriptor.name })
            assertFalse(tools.first().descriptor.description.isNullOrBlank())
            assertTrue(tools.first().descriptor.inputSchema.propertiesSchema.containsKey("appId"))
        }
    }
}
