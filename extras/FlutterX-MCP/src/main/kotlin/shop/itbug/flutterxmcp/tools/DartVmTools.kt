package shop.itbug.flutterxmcp.tools

import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import shop.itbug.flutterx.common.dart.FlutterXVMService
import vm.getVm
import vm.network.NetworkRequest
import vm.sp.AsyncState
import vm.sp.SharedPreferencesServices

/**
 * FlutterX MCP Tools - Dart VM 相关工具集
 *
 * 提供以下功能:
 * - 获取当前运行的 Flutter 应用列表
 * - 获取 Dart VM 信息
 * - 获取 HTTP 请求监控数据
 * - 获取 SharedPreferences 缓存数据
 */
class DartVmTools(private val project: Project) {

    private val vmService: FlutterXVMService
        get() = FlutterXVMService.getInstance(project)

    /**
     * 注册所有 MCP 工具到 Server
     */
    fun registerTools(server: Server) {
        // 获取运行中的 Flutter 应用列表
        server.addTool(
            name = "get_running_flutter_apps",
            description = "获取当前 IDE 中正在运行的所有 Flutter 应用列表，包括应用ID、设备ID、运行模式和 VM 服务 URL",
        ) {
            getRunningFlutterApps()
        }

        // 获取 Dart VM 信息
        server.addTool(
            name = "get_dart_vm_info",
            description = "获取指定 Flutter 应用的 Dart VM 详细信息，包括版本、isolates、架构等",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("appId") {
                        put("type", "string")
                        put("description", "Flutter 应用 ID，可通过 get_running_flutter_apps 获取")
                    }
                },
                required = listOf("appId")
            )
        ) { request ->
            val appId = request.arguments?.get("appId")?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: appId")
            getDartVmInfo(appId)
        }


        // 获取 HTTP 请求列表
        server.addTool(
            name = "get_http_requests",
            description = "获取指定 Flutter 应用的 HTTP 请求监控数据，包括请求URL、方法、状态码、耗时等",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("appId") {
                        put("type", "string")
                        put("description", "Flutter 应用 ID")
                    }
                    putJsonObject("limit") {
                        put("type", "integer")
                        put("description", "返回的最大请求数量，默认 50")
                    }
                    putJsonObject("method") {
                        put("type", "string")
                        put("description", "按 HTTP 方法过滤 (GET/POST/PUT/DELETE 等)")
                    }
                    putJsonObject("urlContains") {
                        put("type", "string")
                        put("description", "按 URL 包含的字符串过滤")
                    }
                },
                required = listOf("appId")
            )
        ) { request ->
            val args = request.arguments
            val appId = args?.get("appId")?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: appId")
            val limit = args["limit"]?.jsonPrimitive?.intOrNull ?: 50
            val method = args["method"]?.jsonPrimitive?.contentOrNull
            val urlContains = args["urlContains"]?.jsonPrimitive?.contentOrNull
            getHttpRequests(appId, limit, method, urlContains)
        }

        // 获取 HTTP 请求详情
        server.addTool(
            name = "get_http_request_detail",
            description = "获取指定 HTTP 请求的详细信息，包括请求体和响应体",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("appId") {
                        put("type", "string")
                        put("description", "Flutter 应用 ID")
                    }
                    putJsonObject("requestId") {
                        put("type", "string")
                        put("description", "HTTP 请求 ID，可通过 get_http_requests 获取")
                    }
                },
                required = listOf("appId", "requestId")
            )
        ) { request ->
            val args = request.arguments
            val appId = args?.get("appId")?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: appId")
            val requestId = args["requestId"]?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: requestId")
            getHttpRequestDetail(appId, requestId)
        }

        // 获取 SharedPreferences 所有 keys
        server.addTool(
            name = "get_shared_preferences_keys",
            description = "获取指定 Flutter 应用的 SharedPreferences 中所有存储的 key 列表",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("appId") {
                        put("type", "string")
                        put("description", "Flutter 应用 ID")
                    }
                    putJsonObject("legacy") {
                        put("type", "boolean")
                        put("description", "是否使用 legacy API，默认 false")
                    }
                },
                required = listOf("appId")
            )
        ) { request ->
            val args = request.arguments
            val appId = args?.get("appId")?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: appId")
            val legacy = args["legacy"]?.jsonPrimitive?.booleanOrNull ?: false
            getSharedPreferencesKeys(appId, legacy)
        }

        // 获取 SharedPreferences 指定 key 的值
        server.addTool(
            name = "get_shared_preferences_value",
            description = "获取指定 Flutter 应用的 SharedPreferences 中某个 key 的值",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("appId") {
                        put("type", "string")
                        put("description", "Flutter 应用 ID")
                    }
                    putJsonObject("key") {
                        put("type", "string")
                        put("description", "要获取的 key 名称")
                    }
                    putJsonObject("legacy") {
                        put("type", "boolean")
                        put("description", "是否使用 legacy API，默认 false")
                    }
                },
                required = listOf("appId", "key")
            )
        ) { request ->
            val args = request.arguments
            val appId = args?.get("appId")?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: appId")
            val key = args["key"]?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: key")
            val legacy = args["legacy"]?.jsonPrimitive?.booleanOrNull ?: false
            getSharedPreferencesValue(appId, key, legacy)
        }

        // 获取 SharedPreferences 所有键值对
        server.addTool(
            name = "get_shared_preferences_all",
            description = "获取指定 Flutter 应用的 SharedPreferences 中所有键值对，以 key-value 形式返回",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    putJsonObject("appId") {
                        put("type", "string")
                        put("description", "Flutter 应用 ID")
                    }
                    putJsonObject("legacy") {
                        put("type", "boolean")
                        put("description", "是否使用 legacy API，默认 false")
                    }
                },
                required = listOf("appId")
            )
        ) { request ->
            val args = request.arguments
            val appId = args?.get("appId")?.jsonPrimitive?.content
                ?: return@addTool errorResult("缺少必需参数: appId")
            val legacy = args["legacy"]?.jsonPrimitive?.booleanOrNull ?: false
            getSharedPreferencesAll(appId, legacy)
        }

        // 获取当前项目名称
        server.addTool(
            name = "get_current_project_name",
            description = "获取当前 IDE 打开的项目名称",
        ) {
            getCurrentProjectName()
        }
    }

    // ==================== 工具实现 ====================

    private fun getRunningFlutterApps(): CallToolResult {
        val apps = vmService.allFlutterApps
        if (apps.isEmpty()) {
            return CallToolResult(
                content = listOf(TextContent("当前没有运行中的 Flutter 应用")),
                isError = false
            )
        }

        val appsJson = buildJsonArray {
            apps.forEach { app ->
                add(buildJsonObject {
                    put("appId", app.appInfo.appId)
                    put("deviceId", app.appInfo.deviceId)
                    put("mode", app.appInfo.mode)
                    put("vmUrl", app.appInfo.vmUrl)
                })
            }
        }

        return CallToolResult(
            content = listOf(TextContent(appsJson.toString())),
            isError = false
        )
    }

    private fun getDartVmInfo(appId: String): CallToolResult {
        val app = vmService.getFlutterAppByAppId(appId)
            ?: return errorResult("未找到应用: $appId")

        return runBlocking {
            try {
                val vm = app.vmService.getVm()
                val vmJson = buildJsonObject {
                    put("name", vm.getName() ?: "unknown")
                    put("version", vm.getVersion() ?: "unknown")
                    put("architectureBits", vm.getArchitectureBits())
                    put("hostCPU", vm.getHostCPU() ?: "unknown")
                    put("operatingSystem", vm.getOperatingSystem() ?: "unknown")
                    put("targetCPU", vm.getTargetCPU() ?: "unknown")
                    put("pid", vm.getPid())
                    put("startTime", vm.getStartTime())

                    // 内存信息
                    put("memory", buildJsonObject {
                        put("currentMemory", vm.getCurrentMemory())
                        put("currentMemoryMB", String.format("%.1f", vm.getCurrentMemory() / 1024.0 / 1024.0))
                        put("currentRSS", vm.getCurrentRSS())
                        put("currentRSSMB", String.format("%.1f", vm.getCurrentRSS() / 1024.0 / 1024.0))
                        put("maxRSS", vm.getMaxRSS())
                        put("maxRSSMB", String.format("%.1f", vm.getMaxRSS() / 1024.0 / 1024.0))
                    })

                    // Isolates 信息
                    put("isolates", buildJsonArray {
                        vm.getIsolates().forEach { isolate ->
                            add(buildJsonObject {
                                put("id", isolate.getId() ?: "")
                                put("name", isolate.getName() ?: "")
                                put("number", isolate.getNumber() ?: "")
                                put("isSystemIsolate", isolate.getIsSystemIsolate())
                            })
                        }
                    })

                    // Isolate Groups 信息
                    put("isolateGroups", buildJsonArray {
                        vm.getIsolateGroups().forEach { group ->
                            add(buildJsonObject {
                                put("id", group.getId() ?: "")
                                put("name", group.getName() ?: "")
                                put("number", group.getNumber() ?: "")
                                put("isSystemIsolateGroup", group.getIsSystemIsolateGroup())
                            })
                        }
                    })
                }

                CallToolResult(
                    content = listOf(TextContent(vmJson.toString())),
                    isError = false
                )
            } catch (e: Exception) {
                errorResult("获取 VM 信息失败: ${e.message}")
            }
        }
    }

    private fun getHttpRequests(
        appId: String,
        limit: Int,
        method: String?,
        urlContains: String?
    ): CallToolResult {
        val app = vmService.getFlutterAppByAppId(appId)
            ?: return errorResult("未找到应用: $appId")

        val monitor = app.vmService.dartHttpMonitor
        val requests = monitor.filterRequests(
            method = method,
            containsUrl = urlContains
        ).take(limit)

        if (requests.isEmpty()) {
            return CallToolResult(
                content = listOf(TextContent("没有找到匹配的 HTTP 请求")),
                isError = false
            )
        }

        val requestsJson = buildJsonArray {
            requests.forEach { req ->
                add(req.toJsonObject())
            }
        }

        return CallToolResult(
            content = listOf(TextContent(requestsJson.toString())),
            isError = false
        )
    }

    private fun getHttpRequestDetail(appId: String, requestId: String): CallToolResult {
        val app = vmService.getFlutterAppByAppId(appId)
            ?: return errorResult("未找到应用: $appId")

        val monitor = app.vmService.dartHttpMonitor

        return runBlocking {
            try {
                val request = monitor.getRequestDetails(requestId)
                    ?: return@runBlocking errorResult("未找到请求: $requestId")

                val detailJson = request.toDetailJsonObject()

                CallToolResult(
                    content = listOf(TextContent(detailJson.toString())),
                    isError = false
                )
            } catch (e: Exception) {
                errorResult("获取请求详情失败: ${e.message}")
            }
        }
    }

    private fun getSharedPreferencesKeys(appId: String, legacy: Boolean): CallToolResult {
        val app = vmService.getFlutterAppByAppId(appId)
            ?: return errorResult("未找到应用: $appId")

        val spService = SharedPreferencesServices(app.vmService)

        return runBlocking {
            try {
                spService.fetchAllKeys()
                if (legacy) {
                    spService.selectApi(true)
                }

                val state = spService.state.value
                when (val keysState = state.allKeys) {
                    is AsyncState.Loading -> {
                        errorResult("正在加载中...")
                    }

                    is AsyncState.Error -> {
                        errorResult("获取 keys 失败: ${keysState.error.message}")
                    }

                    is AsyncState.Data -> {
                        val keysJson = buildJsonObject {
                            put("keys", buildJsonArray {
                                keysState.data.forEach { add(it) }
                            })
                            put("count", keysState.data.size)
                            put("apiType", if (legacy) "legacy" else "async")
                        }
                        CallToolResult(
                            content = listOf(TextContent(keysJson.toString())),
                            isError = false
                        )
                    }
                }
            } catch (e: Exception) {
                errorResult("获取 SharedPreferences keys 失败: ${e.message}")
            } finally {
                spService.dispose()
            }
        }
    }

    private fun getSharedPreferencesValue(appId: String, key: String, legacy: Boolean): CallToolResult {
        val app = vmService.getFlutterAppByAppId(appId)
            ?: return errorResult("未找到应用: $appId")

        val spService = SharedPreferencesServices(app.vmService)

        return runBlocking {
            try {
                if (legacy) {
                    spService.selectApi(true)
                }
                spService.selectKey(key)

                val state = spService.state.value
                val selectedKey = state.selectedKey
                    ?: return@runBlocking errorResult("未能选择 key: $key")

                when (val valueState = selectedKey.value) {
                    is AsyncState.Loading -> {
                        errorResult("正在加载中...")
                    }

                    is AsyncState.Error -> {
                        errorResult("获取值失败: ${valueState.error.message}")
                    }

                    is AsyncState.Data -> {
                        val valueJson = buildJsonObject {
                            put("key", key)
                            put("type", valueState.data.kind)
                            put("value", valueState.data.valueAsString)
                        }
                        CallToolResult(
                            content = listOf(TextContent(valueJson.toString())),
                            isError = false
                        )
                    }
                }
            } catch (e: Exception) {
                errorResult("获取 SharedPreferences 值失败: ${e.message}")
            } finally {
                spService.dispose()
            }
        }
    }

    private fun getSharedPreferencesAll(appId: String, legacy: Boolean): CallToolResult {
        val app = vmService.getFlutterAppByAppId(appId)
            ?: return errorResult("未找到应用: $appId")

        val spService = SharedPreferencesServices(app.vmService)

        return runBlocking {
            try {
                spService.fetchAllKeys()
                if (legacy) {
                    spService.selectApi(true)
                }

                val state = spService.state.value
                when (val keysState = state.allKeys) {
                    is AsyncState.Loading -> {
                        return@runBlocking errorResult("正在加载 keys...")
                    }

                    is AsyncState.Error -> {
                        return@runBlocking errorResult("获取 keys 失败: ${keysState.error.message}")
                    }

                    is AsyncState.Data -> {
                        val keys = keysState.data
                        val allValues = mutableMapOf<String, JsonObject>()

                        // 遍历所有 key 获取值
                        for (key in keys) {
                            try {
                                spService.selectKey(key)
                                val currentState = spService.state.value
                                val selectedKey = currentState.selectedKey
                                if (selectedKey != null) {
                                    when (val valueState = selectedKey.value) {
                                        is AsyncState.Data -> {
                                            allValues[key] = buildJsonObject {
                                                put("type", valueState.data.kind)
                                                put("value", valueState.data.valueAsString)
                                            }
                                        }

                                        is AsyncState.Error -> {
                                            allValues[key] = buildJsonObject {
                                                put("type", "error")
                                                put("value", valueState.error.message ?: "unknown error")
                                            }
                                        }

                                        is AsyncState.Loading -> {
                                            allValues[key] = buildJsonObject {
                                                put("type", "loading")
                                                put("value", "...")
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                allValues[key] = buildJsonObject {
                                    put("type", "error")
                                    put("value", e.message ?: "unknown error")
                                }
                            }
                        }

                        val resultJson = buildJsonObject {
                            put("count", keys.size)
                            put("apiType", if (legacy) "legacy" else "async")
                            put("data", buildJsonObject {
                                allValues.forEach { (k, v) ->
                                    put(k, v)
                                }
                            })
                        }

                        CallToolResult(
                            content = listOf(TextContent(resultJson.toString())),
                            isError = false
                        )
                    }
                }
            } catch (e: Exception) {
                errorResult("获取 SharedPreferences 全部数据失败: ${e.message}")
            } finally {
                spService.dispose()
            }
        }
    }

    private fun getCurrentProjectName(): CallToolResult {
        val projectName = project.name
        val basePath = project.basePath

        val result = buildJsonObject {
            put("name", projectName)
            put("basePath", basePath ?: "unknown")
        }

        return CallToolResult(
            content = listOf(TextContent(result.toString())),
            isError = false
        )
    }

    // ==================== 辅助方法 ====================

    private fun errorResult(message: String): CallToolResult {
        return CallToolResult(
            content = listOf(TextContent(message)),
            isError = true
        )
    }

    private fun NetworkRequest.toJsonObject(): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("method", method)
            put("uri", uri)
            put("startTime", requestStartTime)
            put("status", status.name)
            put("statusCode", statusCode ?: -1)
            put("duration", durationMs)
            put("isComplete", isComplete)
            put("error", error)
        }
    }

    private fun NetworkRequest.toDetailJsonObject(): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("method", method)
            put("uri", uri)
            put("startTime", requestStartTime)
            put("status", status.name)
            put("statusCode", statusCode ?: -1)
            put("duration", durationMs)
            put("isComplete", isComplete)
            put("error", error)

            // 请求头
            put("requestHeaders", buildJsonObject {
                requestHeaders?.forEach { (k, v) ->
                    put(k, v)
                }
            })

            // 响应头
            put("responseHeaders", buildJsonObject {
                responseHeaders?.forEach { (k, v) ->
                    put(k, v)
                }
            })

            // 请求体
            put("requestBody", requestBody)

            // 响应体
            put("responseBody", responseBody)

            // 查询参数
            put("queryParams", buildJsonObject {
                queryParams.forEach { (k, v) ->
                    put(k, v?.toString() ?: "null")
                }
            })

            // 事件列表
            put("events", buildJsonArray {
                events.forEach { event ->
                    add(buildJsonObject {
                        put("timestamp", event.timestamp)
                        put("event", event.event)
                        put("time", event.time1)
                    })
                }
            })
        }
    }
}
