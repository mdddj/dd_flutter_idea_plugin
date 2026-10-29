package shop.itbug.flutterx.tools

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import java.io.File

/**
 * 一个可用的 iOS 模拟器，数据来自 `xcrun simctl list devices available --json`。
 */
internal data class IosSimulator(
    val udid: String,
    val name: String,
    val runtime: String,
    val state: String,
) {
    val isBooted: Boolean get() = state.equals("Booted", ignoreCase = true)

    override fun toString(): String = buildString {
        append(name)
        if (runtime.isNotBlank()) {
            append("  (").append(runtime).append(')')
        }
        if (isBooted) {
            append("  ● Booted")
        }
    }
}

/**
 * 模拟器的 UI 宿主应用。
 *
 * Xcode 26 及更早是 `Simulator.app`；Xcode 27 起点 `Simulator.app` 被 `DeviceHub.app`
 * (`com.apple.dt.Devices`) 取代。旧的 `open -a Simulator` 在新系统上会直接报
 * "Unable to find application named 'Simulator'"，所以这里按绝对路径唤起。
 */
private data class SimulatorHost(val path: String, val supportsDeviceArgument: Boolean)

/**
 * 通过 `xcrun simctl` 启动 Xcode 的 iOS 模拟器，仅 macOS 可用。
 *
 * 所有方法都是阻塞的，调用方需要在后台线程执行（参见 [shop.itbug.flutterx.util.launchBackgroundProgress]）。
 */
internal object IosSimulatorTool {

    private const val XCODE_SELECT = "/usr/bin/xcode-select"

    private val UI_BUNDLE_IDS = listOf("com.apple.iphonesimulator", "com.apple.dt.Devices")

    /**
     * `xcrun` 只存在于 macOS（随 Xcode / Command Line Tools 安装），因此「可执行文件存在」
     * 本身就是「这台机器能启动 iOS 模拟器」的判定条件，不需要再做系统判断。
     */
    private val xcrunExecutable: String? by lazy {
        listOf("/usr/bin/xcrun", "/usr/local/bin/xcrun").firstOrNull { File(it).canExecute() }
    }

    /**
     * 当前机器是否具备启动 iOS 模拟器的条件（macOS + 已安装 Xcode 命令行工具）。
     */
    fun isAvailable(): Boolean = xcrunExecutable != null

    /**
     * 当前生效的 Xcode developer 目录，例如 `/Applications/Xcode.app/Contents/Developer`。
     */
    private val developerDirectory: String by lazy {
        val output = runCatching { ExecUtil.execAndGetOutput(GeneralCommandLine(XCODE_SELECT, "-p")) }.getOrNull()
        output?.stdout?.trim()?.takeIf { output.exitCode == 0 && it.isNotBlank() }
            ?: "/Applications/Xcode.app/Contents/Developer"
    }

    /**
     * 按 Xcode 版本差异依次尝试 `Simulator.app` 与 `DeviceHub.app`。
     */
    private val simulatorHost: SimulatorHost? by lazy {
        val developer = developerDirectory
        val xcodeContents = File(developer).parentFile?.path
        val candidates = buildList {
            add(SimulatorHost("$developer/Applications/Simulator.app", supportsDeviceArgument = true))
            if (xcodeContents != null) {
                add(SimulatorHost("$xcodeContents/Applications/Simulator.app", supportsDeviceArgument = true))
            }
            add(SimulatorHost("/Applications/Simulator.app", supportsDeviceArgument = true))
            add(SimulatorHost("$developer/Applications/DeviceHub.app", supportsDeviceArgument = false))
            if (xcodeContents != null) {
                add(SimulatorHost("$xcodeContents/Applications/DeviceHub.app", supportsDeviceArgument = false))
            }
            add(SimulatorHost("/Applications/DeviceHub.app", supportsDeviceArgument = false))
        }
        candidates.firstOrNull { File(it.path).isDirectory }
    }

    /**
     * 拉取所有可用（available）的 iOS 模拟器，按 iOS 版本倒序、同版本按名称排序。
     *
     * @throws IllegalStateException 当 `xcrun` 执行失败时
     */
    fun listSimulators(): List<IosSimulator> {
        val command = generalCommandLine("simctl", "list", "devices", "available", "--json")
            ?: error("xcrun not found")
        val output = ExecUtil.execAndGetOutput(command)
        if (output.exitCode != 0) {
            throw IllegalStateException(
                output.stderr.ifBlank { output.stdout }.trim().ifBlank {
                    "xcrun simctl list failed with exit code ${output.exitCode}"
                }
            )
        }
        return parseSimulators(output.stdout)
    }

    /**
     * 启动指定模拟器：先 `simctl boot`（已启动则跳过），再唤起模拟器 UI（Simulator.app 或 DeviceHub.app）。
     *
     * @return 出错时的提示信息，成功返回 null
     */
    fun boot(simulator: IosSimulator): String? {
        if (!simulator.isBooted) {
            val command = generalCommandLine("simctl", "boot", simulator.udid)
                ?: return "xcrun not found"
            val output = ExecUtil.execAndGetOutput(command)
            if (output.exitCode != 0) {
                val message = output.stderr.ifBlank { output.stdout }.trim()
                // 已经处于 Booted / Booting 状态时不算失败
                if (!isAlreadyBootedMessage(message)) {
                    return message.ifBlank { "Boot failed with exit code ${output.exitCode}" }
                }
            }
        }

        return openSimulatorUi(simulator.udid)
    }

    /**
     * 唤起模拟器 UI：优先用绝对路径打开（避免 `open -a Simulator` 在新 Xcode 上找不到应用），
     * 失败时再按 bundle id 兜底（兼容非默认安装路径）。
     */
    private fun openSimulatorUi(udid: String): String? {
        val host = simulatorHost
        if (host != null) {
            val command = GeneralCommandLine("open", "-a", host.path)
            if (host.supportsDeviceArgument) {
                command.withParameters("--args", "-CurrentDeviceUDID", udid)
            }
            val output = ExecUtil.execAndGetOutput(command)
            if (output.exitCode == 0) return null
        }

        for (bundleId in UI_BUNDLE_IDS) {
            val output = ExecUtil.execAndGetOutput(GeneralCommandLine("open", "-b", bundleId))
            if (output.exitCode == 0) return null
        }

        return "Unable to launch the iOS Simulator UI. Open it once from Xcode \u25b8 Open Developer Tool."
    }

    /**
     * 解析 `simctl list devices --json` 的输出，只保留 iOS runtime 下可用的设备。
     */
    fun parseSimulators(json: String): List<IosSimulator> {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return emptyList()
        val devices = root.get("devices")?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()

        val result = mutableListOf<IosSimulator>()
        for ((runtimeKey, value) in devices.entrySet()) {
            val runtime = runtimeDisplayName(runtimeKey) ?: continue
            val array = value.takeIf { it.isJsonArray }?.asJsonArray ?: continue
            for (element in array) {
                val device = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                if (device.booleanValue("isAvailable") == false) continue
                val udid = device.stringValue("udid") ?: continue
                val name = device.stringValue("name") ?: continue
                result += IosSimulator(
                    udid = udid,
                    name = name,
                    runtime = runtime,
                    state = device.stringValue("state") ?: "Shutdown",
                )
            }
        }

        return result.sortedWith(
            compareByDescending<IosSimulator> { runtimeVersion(it.runtime) }.thenBy { it.name.lowercase() }
        )
    }

    /**
     * `com.apple.CoreSimulator.SimRuntime.iOS-18-2` -> `iOS 18.2`；非 iOS 返回 null。
     */
    private fun runtimeDisplayName(runtimeKey: String): String? {
        val raw = runtimeKey.substringAfterLast("SimRuntime.", runtimeKey)
        val segments = raw.split('-')
        val os = segments.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (!os.equals("iOS", ignoreCase = true)) return null
        val version = segments.drop(1).joinToString(".")
        return if (version.isBlank()) os else "$os $version"
    }

    /**
     * 把 `iOS 18.2` 这类版本号压成可比较的整数（18.2 -> 1802）。
     */
    private fun runtimeVersion(runtime: String): Int {
        val version = runtime.substringAfter(' ', "")
        return version.split('.').mapNotNull { it.toIntOrNull() }
            .fold(0) { acc, part -> acc * 100 + part }
    }

    private fun isAlreadyBootedMessage(message: String): Boolean {
        val lower = message.lowercase()
        return "current state: booted" in lower || "current state: booting" in lower
    }

    private fun generalCommandLine(vararg parameters: String): GeneralCommandLine? {
        val executable = xcrunExecutable ?: return null
        return GeneralCommandLine(executable).withParameters(*parameters)
    }

    private fun com.google.gson.JsonObject.stringValue(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun com.google.gson.JsonObject.booleanValue(key: String): Boolean? =
        get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrNull()
}
