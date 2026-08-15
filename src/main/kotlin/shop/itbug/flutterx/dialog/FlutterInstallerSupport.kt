package shop.itbug.flutterx.dialog

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import shop.itbug.flutterx.tools.FlutterVersionTool
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal enum class FlutterArtifactPlatform {
    ANDROID,
    IOS,
}

internal data class FlutterInstallArtifact(
    val path: Path,
    val platform: FlutterArtifactPlatform,
    val isAppBundle: Boolean = false,
    val displayPath: String = path.toString(),
) {
    override fun toString(): String = displayPath
}

internal data class FlutterInstallDevice(
    val id: String,
    val name: String,
    val platform: FlutterArtifactPlatform,
    val isSimulator: Boolean,
    val targetType: String?,
) {
    override fun toString(): String = buildString {
        append(name.ifBlank { id })
        append(" [")
        append(platform.name.lowercase())
        if (!targetType.isNullOrBlank()) {
            append(" / ")
            append(targetType)
        }
        append("] - ")
        append(id)
    }
}

internal object FlutterInstallerSupport {
    private val skippedDirectoryNames = setOf(
        ".dart_tool",
        ".git",
        ".gradle",
        ".idea",
        "node_modules",
        "Pods",
    )

    fun flutterExecutable(project: Project): String {
        val sdkHome = runCatching {
            ReadAction.nonBlocking<String?> { FlutterVersionTool.getFlutterHome(project)?.path }
                .expireWith(project)
                .executeSynchronously()
        }.getOrNull()
        if (!sdkHome.isNullOrBlank()) {
            val executable = File(sdkHome, "bin/flutter")
            if (executable.isFile) {
                return executable.path
            }
            val windowsExecutable = File(sdkHome, "bin/flutter.bat")
            if (windowsExecutable.isFile) {
                return windowsExecutable.path
            }
        }
        return "flutter"
    }

    fun scanArtifacts(root: Path, indicator: ProgressIndicator? = null): List<FlutterInstallArtifact> {
        if (!Files.isDirectory(root)) return emptyList()

        val artifacts = mutableListOf<FlutterInstallArtifact>()
        try {
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    indicator?.checkCanceled()
                    if (dir != root && dir.fileName.toString() in skippedDirectoryNames) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    if (dir != root && isIosAppBundle(root, dir)) {
                        artifacts += FlutterInstallArtifact(
                            path = dir,
                            platform = FlutterArtifactPlatform.IOS,
                            isAppBundle = true,
                            displayPath = root.relativize(dir).toString(),
                        )
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    indicator?.checkCanceled()
                    when (file.fileName.toString().substringAfterLast('.', "").lowercase()) {
                        "apk" -> artifacts += FlutterInstallArtifact(
                            file,
                            FlutterArtifactPlatform.ANDROID,
                            displayPath = root.relativize(file).toString(),
                        )
                        "ipa" -> artifacts += FlutterInstallArtifact(
                            file,
                            FlutterArtifactPlatform.IOS,
                            displayPath = root.relativize(file).toString(),
                        )
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE
            })
        } catch (_: IOException) {
            // A partially readable project is still useful to the installer.
        }

        return artifacts.sortedBy { root.relativize(it.path).toString() }
    }

    private fun isIosAppBundle(root: Path, directory: Path): Boolean {
        if (!directory.fileName.toString().endsWith(".app", ignoreCase = true)) return false
        val pathSegments = root.relativize(directory).map { it.toString().lowercase() }
        return pathSegments.any { it == "ios" || it == "iphoneos" || it == "iphonesimulator" }
    }

    fun scanDevices(project: Project, indicator: ProgressIndicator? = null): List<FlutterInstallDevice> {
        val projectDirectory = project.guessProjectDir()?.toNioPath()?.toFile()
        val command = GeneralCommandLine(flutterExecutable(project), "devices", "--machine")
            .withWorkDirectory(projectDirectory)
        indicator?.text = "Scanning Flutter devices"
        val output = ExecUtil.execAndGetOutput(command)
        if (output.exitCode != 0) {
            throw IllegalStateException(output.stderr.ifBlank { output.stdout }.trim().ifBlank {
                "flutter devices failed with exit code ${output.exitCode}"
            })
        }
        return parseDevices(output.stdout)
    }

    fun parseDevices(output: String): List<FlutterInstallDevice> {
        val jsonStart = output.indexOf('[')
        val jsonEnd = output.lastIndexOf(']')
        if (jsonStart < 0 || jsonEnd <= jsonStart) return emptyList()

        val array = runCatching { JsonParser.parseString(output.substring(jsonStart, jsonEnd + 1)).asJsonArray }
            .getOrElse { return emptyList() }

        return array.mapNotNull { element ->
            val device = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val id = device.stringValue("id")?.trim().orEmpty()
            if (id.isBlank()) return@mapNotNull null
            val platformText = listOf("targetPlatform", "platform")
                .firstNotNullOfOrNull { key -> device.stringValue(key) }
                ?.lowercase()
                ?: return@mapNotNull null
            val platform = when {
                platformText.contains("android") -> FlutterArtifactPlatform.ANDROID
                platformText.contains("ios") -> FlutterArtifactPlatform.IOS
                else -> return@mapNotNull null
            }
            if (device.booleanValue("isSupported") == false) return@mapNotNull null
            val targetType = device.stringValue("targetType")
            val isSimulator = targetType.equals("simulator", ignoreCase = true) ||
                device.booleanValue("emulator") == true
            FlutterInstallDevice(
                id = id,
                name = device.stringValue("name")?.trim().orEmpty(),
                platform = platform,
                isSimulator = isSimulator,
                targetType = targetType ?: if (isSimulator) "simulator" else "physical",
            )
        }.distinctBy { it.id }
    }

    fun install(
        project: Project,
        artifact: FlutterInstallArtifact,
        device: FlutterInstallDevice,
        indicator: ProgressIndicator? = null,
    ): ProcessOutput {
        require(artifact.platform == device.platform) {
            "${artifact.platform.name.lowercase()} artifact cannot be installed on a ${device.platform.name.lowercase()} device"
        }
        require(!artifact.isAppBundle || device.isSimulator) {
            "An .ipa file is required to install on a physical iOS device"
        }
        if (artifact.isAppBundle && device.platform == FlutterArtifactPlatform.IOS && device.isSimulator) {
            val command = GeneralCommandLine("xcrun", "simctl", "install", device.id, artifact.path.toString())
                .withWorkDirectory(project.guessProjectDir()?.toNioPath()?.toFile())
            indicator?.text = "Installing ${artifact.path.fileName}"
            return ExecUtil.execAndGetOutput(command)
        }

        val command = GeneralCommandLine(flutterExecutable(project), "install")
            .withParameters(
                "--device-id", device.id,
                "--use-application-binary", artifact.path.toString(),
            )
            .withWorkDirectory(project.guessProjectDir()?.toNioPath()?.toFile())
        indicator?.text = "Installing ${artifact.path.fileName}"
        return ExecUtil.execAndGetOutput(command)
    }

    private fun com.google.gson.JsonObject.stringValue(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString

    private fun com.google.gson.JsonObject.booleanValue(key: String): Boolean? =
        get(key)?.takeIf { it.isJsonPrimitive }?.runCatching { asBoolean }?.getOrNull()
}
