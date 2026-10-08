package shop.itbug.flutterx.api.vm

import com.intellij.mcpserver.McpToolset

/**
 * 可选扩展能力：实现该接口的 Dart VM DevTool 会把自己的 MCP 工具自动注册给 AI。
 *
 * 不实现这个接口（或者 [createMcpToolset] 返回 null）表示该 Tab 不对 AI 暴露数据。
 *
 * 实现约定：
 * 1. 工具集必须**无状态**。`McpToolset` 实例只在插件启动时创建一次，
 *    每次调用都要通过 [shop.itbug.flutterx.mcp.DartVmMcpToolset] 提供的
 *    `currentProject()` / `runningApps()` / `requireApp(appId)` 现取数据。
 * 2. `@McpTool` 方法建议直接用 snake_case 函数名作为工具名，并加 `flutterx_` 前缀，
 *    避免和其他插件的 MCP 工具重名。
 * 3. 返回类型用 `String`（kotlinx.serialization 自带 serializer），内容放 JSON 文本。
 *
 * 完整示例见 `DartVmSharedPreferencesMcpToolset`。
 */
interface DartVmMcpToolExtension {
    fun createMcpToolset(): McpToolset?
}
