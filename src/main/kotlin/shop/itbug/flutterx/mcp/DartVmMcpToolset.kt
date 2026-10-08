package shop.itbug.flutterx.mcp

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.mcpserver.McpExpectedError
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import shop.itbug.flutterx.api.vm.DartVmApp
import shop.itbug.flutterx.common.dart.FlutterXVMService

/**
 * 插件内部所有 Dart VM MCP 工具集的基类，负责三件样板事：
 *
 * 1. 从 MCP 调用上下文里拿到当前 `Project`；
 * 2. 解析 `appId` 参数（缺省取第一个正在运行的 Flutter 应用，参数错误直接给出可用列表）；
 * 3. 把异常转成 MCP 可读的错误文本（[McpExpectedError] 原样抛出）。
 */
abstract class DartVmMcpToolset : McpToolset {

    /** 当前 MCP 调用所属的 Project。 */
    protected suspend fun currentProject(): Project = currentCoroutineContext().project

    /** 当前 Project 里所有已连接 Dart VM 的 Flutter 应用。 */
    protected suspend fun runningApps(): List<DartVmApp> =
        FlutterXVMService.getInstance(currentProject()).runningApps.value

    /**
     * 解析目标 Flutter 应用。
     *
     * @param appId 传 null/空白时取第一个正在运行的应用。
     */
    protected suspend fun requireApp(appId: String? = null): DartVmApp {
        val apps = runningApps()
        if (apps.isEmpty()) {
            mcpFail(
                "No Flutter app is connected to the Dart VM service. " +
                        "Start a Flutter run configuration with the FlutterX VM listener enabled, then retry."
            )
        }
        if (appId.isNullOrBlank()) return apps.first()
        return apps.firstOrNull { it.appId == appId } ?: mcpFail(
            "Unknown appId '$appId'. Running apps: ${apps.joinToString { it.appId }}"
        )
    }

    /** 把调用方传入的条数收进默认值和上限之间。 */
    protected fun clampCount(value: Int?, defaultValue: Int, maxValue: Int): Int =
        (value ?: defaultValue).coerceIn(1, maxValue)

    /** 把工具实现里的异常统一转成 MCP 错误，[McpExpectedError] 直接透传。 */
    protected suspend fun <T> guard(block: suspend () -> T): T = try {
        block()
    } catch (e: McpExpectedError) {
        throw e
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        mcpFail(e.message ?: e.javaClass.simpleName)
    }
}

internal val mcpGson: Gson = GsonBuilder().setPrettyPrinting().create()

/** 统一 JSON 文本输出，AI 侧比 Kotlin data class 更好读。 */
fun Any?.toMcpJson(): String = mcpGson.toJson(this)

/** 截断过长文本，避免一次 MCP 响应把日志正文或状态树整包倒出去。 */
fun String.truncateForMcp(maxChars: Int = 2000): String {
    if (length <= maxChars) return this
    return take(maxChars) + "...(${length - maxChars} more chars)"
}

/** Flutter 应用的对外摘要，所有工具都复用这一份字段。 */
fun DartVmApp.toMcpSummary(): Map<String, Any?> = linkedMapOf(
    "appId" to appId,
    "deviceId" to deviceId,
    "mode" to mode,
    "vmUrl" to vmUrl,
)
