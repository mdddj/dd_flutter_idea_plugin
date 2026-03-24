# FlutterX MCP Server

FlutterX MCP Server 是一个基于 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 的 HTTP 服务器插件，为 AI 助手提供 Dart VM 调试和监控工具。

## 功能特性

- 🚀 获取当前运行的 Flutter 应用列表
- 📊 获取 Dart VM 详细信息（版本、isolates、架构等）
- 🌐 HTTP 请求监控（请求列表、详情、过滤）
- 💾 SharedPreferences 缓存数据读取
- 📁 获取当前项目信息

## 安装依赖

```kotlin
dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.8.1")
    implementation("io.ktor:ktor-server-core:3.3.3")
    implementation("io.ktor:ktor-server-cio:3.3.3")
    implementation("io.ktor:ktor-server-sse:3.3.3")
}
```

## MCP 工具列表

### 1. `get_running_flutter_apps`
获取当前 IDE 中正在运行的所有 Flutter 应用列表。

**参数**: 无

**返回示例**:
```json
[
  {
    "appId": "941182c1-d970-493a-98e8-fbe6d80053c1",
    "deviceId": "macos",
    "mode": "debug",
    "vmUrl": "ws://127.0.0.1:52345/ws"
  }
]
```

### 2. `get_dart_vm_info`
获取指定 Flutter 应用的 Dart VM 详细信息。

**参数**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| appId | string | ✅ | Flutter 应用 ID |

**返回示例**:
```json
{
  "name": "vm",
  "version": "3.5.0",
  "architectureBits": 64,
  "hostCPU": "arm64",
  "operatingSystem": "macos",
  "targetCPU": "arm64",
  "pid": 12345,
  "startTime": 1704326400000,
  "isolates": [...],
  "isolateGroups": [...]
}
```

### 3. `get_http_requests`
获取指定 Flutter 应用的 HTTP 请求监控数据。

**参数**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| appId | string | ✅ | Flutter 应用 ID |
| limit | integer | ❌ | 返回的最大请求数量，默认 50 |
| method | string | ❌ | 按 HTTP 方法过滤 (GET/POST/PUT/DELETE 等) |
| urlContains | string | ❌ | 按 URL 包含的字符串过滤 |

**返回示例**:
```json
[
  {
    "id": "req-123",
    "method": "GET",
    "uri": "https://api.example.com/users",
    "startTime": "2024-01-04 10:30:00:123",
    "status": "COMPLETED",
    "statusCode": 200,
    "duration": 156,
    "isComplete": true
  }
]
```

### 4. `get_http_request_detail`
获取指定 HTTP 请求的详细信息，包括请求体和响应体。

**参数**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| appId | string | ✅ | Flutter 应用 ID |
| requestId | string | ✅ | HTTP 请求 ID |

**返回示例**:
```json
{
  "id": "req-123",
  "method": "POST",
  "uri": "https://api.example.com/login",
  "requestHeaders": {"Content-Type": "application/json"},
  "responseHeaders": {"Content-Type": "application/json"},
  "requestBody": "{\"username\": \"test\"}",
  "responseBody": "{\"token\": \"xxx\"}",
  "queryParams": {},
  "events": [...]
}
```

### 5. `get_shared_preferences_keys`
获取指定 Flutter 应用的 SharedPreferences 中所有存储的 key 列表。

**参数**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| appId | string | ✅ | Flutter 应用 ID |
| legacy | boolean | ❌ | 是否使用 legacy API，默认 false |

**返回示例**:
```json
{
  "keys": ["user_token", "theme_mode", "language"],
  "count": 3,
  "apiType": "async"
}
```

### 6. `get_shared_preferences_value`
获取指定 Flutter 应用的 SharedPreferences 中某个 key 的值。

**参数**:
| 参数名 | 类型 | 必需 | 描述 |
|--------|------|------|------|
| appId | string | ✅ | Flutter 应用 ID |
| key | string | ✅ | 要获取的 key 名称 |
| legacy | boolean | ❌ | 是否使用 legacy API，默认 false |

**返回示例**:
```json
{
  "key": "theme_mode",
  "type": "String",
  "value": "dark"
}
```

### 7. `get_current_project_name`
获取当前 IDE 打开的项目名称。

**参数**: 无

**返回示例**:
```json
{
  "name": "my_flutter_app",
  "basePath": "/Users/dev/projects/my_flutter_app"
}
```

## 使用方式

### 启动 MCP Server

```kotlin
// 获取服务实例
val mcpService = FlutterXMCPService.getInstance(project)

// 启动服务器（默认端口 3000）
mcpService.startServer()

// 或指定端口
mcpService.startServer(port = 8080)

// 停止服务器
mcpService.stopServer()

// 检查是否运行中
val isRunning = mcpService.isRunning()
```

### MCP 客户端配置

在 MCP 客户端（如 Claude Desktop）中配置：

```json
{
  "mcpServers": {
    "flutterx": {
      "type": "sse",
      "url": "http://localhost:5990/"
    }
  }
}
```

## 扩展工具

如需添加新的 MCP 工具，可以在 `DartVmTools.kt` 中添加：

```kotlin
server.addTool(
    name = "your_tool_name",
    description = "工具描述",
    inputSchema = ToolSchema(
        properties = buildJsonObject {
            putJsonObject("param1") {
                put("type", "string")
                put("description", "参数描述")
            }
        },
        required = listOf("param1")
    )
) { request ->
    val param1 = request.arguments?.get("param1")?.jsonPrimitive?.content
    // 实现逻辑
    CallToolResult(
        content = listOf(TextContent("结果")),
        isError = false
    )
}
```

## 依赖项目

- [FlutterX](https://github.com/user/FlutterX) - IntelliJ IDEA Flutter 开发插件
- [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) - Model Context Protocol Kotlin 实现
- [Ktor](https://ktor.io/) - Kotlin 异步 Web 框架

## License

MIT License
