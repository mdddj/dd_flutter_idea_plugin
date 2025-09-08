package vm.network

import com.google.gson.JsonObject
import vm.VmService
import vm.consumer.ServiceExtensionConsumer
import vm.consumer.defaultServiceExtensionConsumer
import vm.element.RPCError
import vm.isHttpProfilingAvailable
import vm.socketProfilingEnable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


// Socket Profiling Extensions

/**
 * 检查Socket分析是否可用
 */
suspend fun VmService.isSocketProfilingAvailable(isolateId: String): Boolean {
    return getIsolateById(isolateId)?.getExtensionRPCs()?.contains("ext.dart.io.getSocketProfile") ?: false
}

/**
 * 获取Socket分析状态
 */
suspend fun VmService.getSocketProfilingState(isolateId: String): JsonObject? {
    if (!isSocketProfilingAvailable(isolateId)) {
        println("Socket分析不可用")
        return null
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.socketProfilingEnabled",
            params = JsonObject(),
            defaultServiceExtensionConsumer({ cont.resume(it) }) { err ->
                cont.resume(null)
            }
        )
    }
}

/**
 * 清除Socket分析数据
 */
suspend fun VmService.clearSocketProfile(isolateId: String): Boolean {
    if (!isSocketProfilingAvailable(isolateId)) {
        println("Socket分析不可用")
        return false
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.clearSocketProfile",
            params = JsonObject(),
            object : ServiceExtensionConsumer {
                override fun received(result: JsonObject) {
                    cont.resume(true)
                }

                override fun onError(error: RPCError) {
                    println("清除Socket分析数据失败: ${error.message}")
                    cont.resume(false)
                }
            }
        )
    }
}

/**
 * 获取Socket分析数据
 */
suspend fun VmService.getSocketProfile(isolateId: String): JsonObject? {
    if (!isSocketProfilingAvailable(isolateId)) {
        println("Socket分析不可用")
        return null
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getSocketProfile",
            params = JsonObject(),
            defaultServiceExtensionConsumer({ cont.resume(it) }) { err ->
                cont.resume(null)
            }
        )
    }
}

// HTTP Timeline Logging Extensions

/**
 * 检查HTTP时间线日志是否可用
 */
suspend fun VmService.isHttpTimelineLoggingAvailable(isolateId: String): Boolean {
    return getIsolateById(isolateId)?.getExtensionRPCs()?.contains("ext.dart.io.httpEnableTimelineLogging") ?: false
}

/**
 * 启用/禁用HTTP时间线日志
 */
suspend fun VmService.setHttpTimelineLogging(isolateId: String, enabled: Boolean): JsonObject? {
    if (!isHttpTimelineLoggingAvailable(isolateId)) {
        println("HTTP时间线日志不可用")
        return null
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.httpEnableTimelineLogging",
            params = JsonObject().apply {
                addProperty("enabled", enabled)
            },
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) { result ->
                cont.resume(null)
            }
        )
    }
}

/**
 * 获取HTTP时间线日志状态
 */
suspend fun VmService.getHttpTimelineLoggingState(isolateId: String): JsonObject? {
    if (!isHttpTimelineLoggingAvailable(isolateId)) {
        println("HTTP时间线日志不可用")
        return null
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.httpEnableTimelineLogging",
            params = JsonObject(),
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

/**
 * 获取特定HTTP请求的详细信息
 */
suspend fun VmService.getHttpProfileRequest(isolateId: String, requestId: String): JsonObject? {
    if (!isHttpProfilingAvailable(isolateId)) {
        println("HTTP分析不可用")
        return null
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getHttpProfileRequest",
            params = JsonObject().apply {
                addProperty("id", requestId)
            },
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

/**
 * 清除HTTP分析数据
 */
suspend fun VmService.clearHttpProfile(isolateId: String): Boolean {
    if (!isHttpProfilingAvailable(isolateId)) {
        println("HTTP分析不可用")
        return false
    }

    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.clearHttpProfile",
            params = JsonObject(),
            object : ServiceExtensionConsumer {
                override fun received(result: JsonObject) {
                    cont.resume(true)
                }

                override fun onError(error: RPCError) {
                    println("清除HTTP分析数据失败: ${error.message}")
                    cont.resume(false)
                }
            }
        )
    }
}

// File Operations Extensions

/**
 * 获取打开的文件列表
 */
suspend fun VmService.getOpenFiles(isolateId: String): JsonObject? {
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getOpenFiles",
            params = JsonObject(),
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

/**
 * 根据ID获取打开文件的详细信息
 */
suspend fun VmService.getOpenFileById(isolateId: String, fileId: Int): JsonObject? {
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getOpenFileById",
            params = JsonObject().apply {
                addProperty("id", fileId)
            },
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

// Process Management Extensions

/**
 * 获取子进程列表
 */
suspend fun VmService.getSpawnedProcesses(isolateId: String): JsonObject? {
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getSpawnedProcesses",
            params = JsonObject(),
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

/**
 * 根据ID获取子进程详细信息
 */
suspend fun VmService.getSpawnedProcessById(isolateId: String, processId: Int): JsonObject? {
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getSpawnedProcessById",
            params = JsonObject().apply {
                addProperty("id", processId)
            },
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

// Version Management Extension

/**
 * 获取Dart IO扩展版本
 */
suspend fun VmService.getDartIOVersion(isolateId: String): JsonObject? {
    return suspendCoroutine { cont ->
        callServiceExtension(
            isolateId = isolateId,
            method = "ext.dart.io.getVersion",
            params = JsonObject(),
            defaultServiceExtensionConsumer({
                cont.resume(it)
            }) {
                cont.resume(null)
            }
        )
    }
}

// Utility Extensions

/**
 * 检查所有IO扩展的可用性
 */
suspend fun VmService.getIOExtensionCapabilities(isolateId: String): IOCapabilities {
    val isolate = getIsolateById(isolateId)
    val extensionRPCs = isolate?.getExtensionRPCs() ?: emptyList()

    return IOCapabilities(
        socketProfiling = extensionRPCs.contains("ext.dart.io.getSocketProfile"),
        httpProfiling = extensionRPCs.contains("ext.dart.io.getHttpProfile"),
        httpTimelineLogging = extensionRPCs.contains("ext.dart.io.httpEnableTimelineLogging"),
        fileOperations = extensionRPCs.contains("ext.dart.io.getOpenFiles"),
        processManagement = extensionRPCs.contains("ext.dart.io.getSpawnedProcesses")
    )
}

/**
 * 批量启用所有可用的IO分析功能
 */
suspend fun VmService.enableAllIOProfiling(isolateId: String): Map<String, Boolean> {
    val capabilities = getIOExtensionCapabilities(isolateId)
    val results = mutableMapOf<String, Boolean>()

    if (capabilities.socketProfiling) {
        try {
            socketProfilingEnable(isolateId, true)
            results["socketProfiling"] = true
        } catch (e: Exception) {
            println("启用Socket分析失败: ${e.message}")
            results["socketProfiling"] = false
        }
    }

    if (capabilities.httpTimelineLogging) {
        try {
            setHttpTimelineLogging(isolateId, true)
            results["httpTimelineLogging"] = true
        } catch (e: Exception) {
            println("启用HTTP时间线日志失败: ${e.message}")
            results["httpTimelineLogging"] = false
        }
    }

    return results
}

/**
 * 批量清除所有IO分析数据
 */
suspend fun VmService.clearAllIOProfiling(isolateId: String): Map<String, Boolean> {
    val capabilities = getIOExtensionCapabilities(isolateId)
    val results = mutableMapOf<String, Boolean>()

    if (capabilities.socketProfiling) {
        results["socketProfile"] = clearSocketProfile(isolateId)
    }

    if (capabilities.httpProfiling) {
        results["httpProfile"] = clearHttpProfile(isolateId)
    }

    return results
}

// Data Classes

/**
 * IO扩展功能可用性
 */
data class IOCapabilities(
    val socketProfiling: Boolean,
    val httpProfiling: Boolean,
    val httpTimelineLogging: Boolean,
    val fileOperations: Boolean,
    val processManagement: Boolean
) {
    fun hasAnyCapability(): Boolean =
        socketProfiling || httpProfiling || httpTimelineLogging || fileOperations || processManagement

    fun getSupportedFeatures(): List<String> = mutableListOf<String>().apply {
        if (socketProfiling) add("Socket分析")
        if (httpProfiling) add("HTTP分析")
        if (httpTimelineLogging) add("HTTP时间线日志")
        if (fileOperations) add("文件操作监控")
        if (processManagement) add("进程管理")
    }
}