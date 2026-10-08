## 示例URL

https://platform.jetbrains.com/t/mcp-integration-in-jetbrains-plugins-recommended-approach-future-support/4171/2


## 代码示例

```kotlin
class FileToolset : McpToolset {
  @McpToolHints(readOnlyHint = TRUE, openWorldHint = FALSE)
  @McpTool
  @McpDescription("""
        |Provides a tree representation of the specified directory in the pseudo graphic format like `tree` utility does.
        |Use this tool to explore the contents of a directory or the whole project.
        |You MUST prefer this tool over listing directories via command line utilities like `ls` or `dir`.
    """)
  suspend fun list_directory_tree(
    @McpDescription(Constants.RELATIVE_PATH_IN_PROJECT_DESCRIPTION) directoryPath: String,
    @McpDescription("Maximum recursion depth") maxDepth: Int = 5,
    @McpDescription(Constants.TIMEOUT_MILLISECONDS_DESCRIPTION) timeout: Int = Constants.MEDIUM_TIMEOUT_MILLISECONDS_VALUE,
  ): DirectoryTreeInfo {
    val project = currentCoroutineContext().project
    val resolvedPath = project.resolveInProject(directoryPath)
    if (!resolvedPath.exists()) mcpFail("No such directory: $resolvedPath")
    if (!resolvedPath.isDirectory()) mcpFail("Not a directory: $resolvedPath")

    val result = StringBuilder()
    val errors = mutableListOf<String>()
    val timedOut = withTimeoutOrNull(timeout.milliseconds) { renderDirectoryTree(resolvedPath.toFile(), result, errors, maxDepth = maxDepth) } == null
    return DirectoryTreeInfo(directoryPath, result.toString(), errors, timedOut)
  }

  @Serializable
  class DirectoryTreeInfo(
    val traversedDirectory: String,
    val tree: String,
    val errors: List<String>,
    @property:McpDescription(Constants.TIMED_OUT_DESCRIPTION)
    @EncodeDefault(mode = EncodeDefault.Mode.NEVER)
    val listingTimedOut: Boolean? = false,
  )

```


## 配置

```xml
<mcpServer.mcpToolset implementation="com.intellij.mcpserver.toolsets.general.FileToolset" />

```
---

## FlutterX Dart VM 数据面板怎么加 MCP 工具

插件已经把这套样板封好了，新增一个面板的 MCP 工具**不需要动 `plugin.xml`**。

### 1. 写工具集

新建 `shop.itbug.flutterx.mcp.tools.XxxMcpToolset`，继承 `DartVmMcpToolset`
（它已经处理好 `Project` 获取、`appId` 解析、异常转错误文本）：

```kotlin
class DartVmHiveMcpToolset : DartVmMcpToolset() {

    @McpTool
    @McpToolHints(readOnlyHint = McpToolHintValue.TRUE, openWorldHint = McpToolHintValue.FALSE)
    @McpDescription("List Hive CE boxes, or read one box. See DartVmHiveMcpToolset for the full parameter list.")
    suspend fun flutterx_hive(
        @McpDescription("Flutter appId from flutterx_list_apps. Omit it to use the first running app.") appId: String? = null,
    ): String = guard {
        val app = requireApp(appId)
        val services = HiveServices(currentProject(), app.vmService)
        try {
            services.initialize()
            mapOf(
                "app" to app.toMcpSummary(),
                "boxes" to services.state.value.boxes.values.map { it.name },
            ).toMcpJson()
        } finally {
            services.dispose()
        }
    }
}
```

要点：
- 方法名（snake_case）就是 MCP 工具名，**加 `flutterx_` 前缀**避免和其他插件重名。
- 返回 `String`，内容用 `toMcpJson()` 输出 JSON 文本。
- 工具集必须**无状态**：实例只创建一次，每次调用都要现取数据。
- `@McpDescription` 必须是编译期常量，**不能用 `.trimMargin()`**，用一行字符串。

### 2. 挂到扩展上

让 DevTool 扩展额外实现 `DartVmMcpToolExtension`：

```kotlin
class DartVmHiveDevToolExtension : DartVmDevToolExtension, DartVmMcpToolExtension {
    override fun getTabTitle(project: Project): String = "Hive CE"

    override fun createComponent(context: DartVmDevToolContext): JComponent { /* ... */ }

    override fun createMcpToolset(): McpToolset = DartVmHiveMcpToolset()
}
```

不实现 `DartVmMcpToolExtension`（或返回 null）＝该面板不对 AI 暴露数据，这是默认行为。

### 3. 自检

- 核心工具由 `FlutterXDartVmMcpTools` 提供：`flutterx_list_apps`、`flutterx_vm_info`、`flutterx_list_data_tools`。
- 扩展工具由 `DartVmExtensionMcpToolsProvider` 自动注册（`mcpServer.mcpToolsProvider` 扩展点）。
- 在 IDE 里执行 `Show MCP Tools` 就能看到工具清单；描述或参数写错时工具会整组静默消失，
  所以加完工具后一定去这个列表里确认一下。
