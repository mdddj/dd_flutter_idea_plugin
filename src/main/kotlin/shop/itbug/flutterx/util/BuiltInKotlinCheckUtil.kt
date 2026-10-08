package shop.itbug.flutterx.util

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets

/**
 * AGP 9.0 built-in Kotlin 支持状态。
 *
 * 判定逻辑与 Flutter 官方迁移指南一致：
 * - AGP 9.0 起移除对 KGP（`kotlin-android` / `org.jetbrains.kotlin.android`）的支持
 * - 已迁移插件：不应用 KGP，`kotlinOptions{}` 替换为顶层 `kotlin { compilerOptions{} }`
 * - 兼容插件：仍应用 KGP，但带 AGP 版本条件判断（`agpMajor < 9` 时才应用）
 */
enum class BuiltInKotlinStatus {
    /** 已迁移：无 KGP、无旧 DSL，可直接在 AGP 9.0 下使用 built-in Kotlin */
    MIGRATED,

    /** 条件式应用 KGP，AGP < 9 时仍可用，AGP 9.0 下兼容 */
    COMPATIBLE,

    /** 未迁移：无条件应用 KGP 或使用旧 DSL，AGP 9.0 下构建会失败 */
    UNMIGRATED,

    /** android 目录下无 Kotlin 源码，不涉及 Kotlin 编译 */
    NO_KOTLIN,
}

data class BuiltInKotlinCheckResult(
    val packageName: String,
    val status: BuiltInKotlinStatus,
    val kotlinFileCount: Int,
    val gradleFiles: List<VirtualFile>,
    val reasons: List<String>,
)

object BuiltInKotlinCheckUtil {

    /** 旧式 KGP 应用语法，覆盖 Groovy 和 Kotlin DSL 的常见写法。 */
    private val LEGACY_KGP = Regex(
        """(?m)(?:apply\s+plugin\s*:\s*["'](?:kotlin-android|org\.jetbrains\.kotlin\.android)["']|id\s*\(?\s*["']org\.jetbrains\.kotlin\.android["']|apply\s*\(\s*plugin\s*=\s*["'](?:kotlin-android|org\.jetbrains\.kotlin\.android)["'])"""
    )

    /** 旧 AGP DSL：`android { kotlinOptions {} }` */
    private val OLD_DSL = Regex("""\bkotlinOptions\b""")

    /** 新 AGP DSL：`kotlin { compilerOptions {} }` */
    private val NEW_DSL = Regex("""\bcompilerOptions\b""")

    /** kapt 注解处理插件 */
    private val KAPT = Regex("""kotlin-kapt|org\.jetbrains\.kotlin\.kapt""")

    /** 条件式应用 KGP 的常见 AGP 版本判断标记。 */
    private val CONDITIONAL_MARKERS = Regex(
        """agpMajor|agpVersion|AGP_VERSION|ANDROID_GRADLE_PLUGIN_VERSION|com\.android\.Version"""
    )

    private val GRADLE_EXTENSIONS = setOf("gradle", "kts", "properties")

    /**
     * 检测已安装 package 的 android 插件部分对 AGP 9.0 built-in Kotlin 的支持状态。
     *
     * @return 包目录或 android 目录不存在时返回 null
     */
    fun checkPackageBuiltInKotlinSupport(project: Project, packageName: String): BuiltInKotlinCheckResult? {
        val packageDirectory =
            DartPackageDirectoryUtil.findInstalledPackageDirectory(project, packageName) ?: return null
        return runReadAction {
            val androidDirectory = packageDirectory.findChild("android") ?: return@runReadAction null
            val gradleFiles = mutableListOf<VirtualFile>()
            collectGradleFiles(androidDirectory, gradleFiles)
            val gradleText = gradleFiles.joinToString("\n") {
                String(it.contentsToByteArray(), StandardCharsets.UTF_8)
            }
            inspect(packageName, gradleText, countKotlinFiles(androidDirectory), gradleFiles)
        }
    }

    /**
     * 纯函数判定：根据 android 目录下所有 Gradle 文件拼接文本与 Kotlin 源码数量得出状态。
     * 与 check_builtin_kotlin.main.kts 的 inspect 逻辑一致。
     */
    fun inspect(
        packageName: String,
        gradleText: String,
        kotlinFileCount: Int,
        gradleFiles: List<VirtualFile> = emptyList(),
    ): BuiltInKotlinCheckResult {
        val reasons = mutableListOf<String>()

        val hasKapt = KAPT.containsMatchIn(gradleText)
        if (hasKapt) reasons += "kapt"
        val hasLegacyKgp = LEGACY_KGP.containsMatchIn(gradleText)
        val hasConditionalKgp = hasLegacyKgp && CONDITIONAL_MARKERS.containsMatchIn(gradleText)
        if (hasLegacyKgp) reasons += if (hasConditionalKgp) "conditional KGP" else "unconditional KGP"
        val hasOldDsl = OLD_DSL.containsMatchIn(gradleText)
        val hasNewDsl = NEW_DSL.containsMatchIn(gradleText)
        if (hasOldDsl) reasons += "kotlinOptions"
        if (hasNewDsl) reasons += "compilerOptions"

        val status = when {
            hasKapt || (hasOldDsl && !hasNewDsl) -> BuiltInKotlinStatus.UNMIGRATED
            hasLegacyKgp && !hasConditionalKgp -> BuiltInKotlinStatus.UNMIGRATED
            hasLegacyKgp -> BuiltInKotlinStatus.COMPATIBLE
            kotlinFileCount == 0 -> BuiltInKotlinStatus.NO_KOTLIN
            else -> BuiltInKotlinStatus.MIGRATED
        }
        return BuiltInKotlinCheckResult(packageName, status, kotlinFileCount, gradleFiles, reasons)
    }

    /** 递归收集 android 目录下的 Gradle 构建文件，跳过 `.gradle` 缓存目录 */
    private fun collectGradleFiles(directory: VirtualFile, result: MutableList<VirtualFile>) {
        for (child in directory.children) {
            if (child.isDirectory) {
                if (child.name != ".gradle") collectGradleFiles(child, result)
            } else if (child.extension in GRADLE_EXTENSIONS) {
                result.add(child)
            }
        }
    }

    private fun countKotlinFiles(directory: VirtualFile): Int {
        var count = 0
        val stack = ArrayDeque<VirtualFile>()
        stack.add(directory)
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            for (child in current.children) {
                if (child.isDirectory) {
                    if (child.name != ".gradle") stack.add(child)
                } else if (child.extension == "kt") {
                    count++
                }
            }
        }
        return count
    }
}
