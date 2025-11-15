package shop.itbug.fluttercheckversionx.tools

import com.google.gson.Gson
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.jetbrains.lang.dart.sdk.DartSdk
import shop.itbug.fluttercheckversionx.model.FlutterLocalVersion
import shop.itbug.fluttercheckversionx.model.FlutterVersionInfo
import java.io.IOException
import java.nio.charset.StandardCharsets


object FlutterVersionTool {

    private const val DART_SDK_SUFFIX = "/bin/cache/dart-sdk"

    /**
     * 获取 flutter安装目录
     */
     fun getFlutterHome(project: Project): VirtualFile? {
        val dartSdkHome = DartSdk.getDartSdk(project) ?: return null
        val dartPath = dartSdkHome.homePath
        if (dartPath.endsWith(DART_SDK_SUFFIX).not()) {
            return null
        }
        val sdkPath: String = dartPath.substring(0, dartPath.length - DART_SDK_SUFFIX.length)
        val home = LocalFileSystem.getInstance().findFileByPath(sdkPath) ?: return null
        return home
    }

    // 获取 flutter版本号 (旧版本 3.38版本以下)
    private suspend fun readVersionFromSdkHome(project: Project): String? {
        val home = getFlutterHome(project) ?: return null
        val versionFile = home.findChild("version") ?: return null
        val versionText = readVersionString(versionFile) ?: return null
        return versionText.trim()
    }

    ///在flutter根目录下读取version版本号
    private suspend fun readVersionString(file: VirtualFile): String? {
        return readAction {
            try {
                val data = String(file.contentsToByteArray(), StandardCharsets.UTF_8)
                for (line in data.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    val t = line.trim { it <= ' ' }
                    if (t.isEmpty() || t.startsWith("#")) {
                        continue
                    }
                    return@readAction line
                }
                return@readAction null
            } catch (_: IOException) {
                return@readAction null
            }
        }
    }


    /**
     * *新版本 3.28.0 中读取版本号的位置变了*
     * 首先，3.38 进行了可能影响自定义生成脚本的关键生成和工具更改，
     * Flutter SDK 根目录的 version 文件已被删除，
     * 取而代之的是位于 bin/cache （#172793） 中的新 flutter.version.json 文件，
     */

    private suspend fun readVersionFromCacheVersionJsonFile(project: Project): FlutterVersionInfo? {
        return readAction {
            val flutterHome: VirtualFile = getFlutterHome(project) ?: return@readAction null
            // 获取 bin 目录下面的 cache目录
            val binFile = flutterHome.findChild("bin") ?: return@readAction null
            val cacheFile = binFile.findChild("cache") ?: return@readAction null
            val versionJsonFile = cacheFile.findChild("flutter.version.json") ?: return@readAction null
            try {
                Gson().fromJson(versionJsonFile.readText(), FlutterVersionInfo::class.java)
            } catch (_: Exception) {
                null
            }
        }
    }

    // 获取本机安装的 flutter版本号
    suspend fun getLocalFlutterVersion(project: Project): FlutterLocalVersion? {
        val versionText = readVersionFromSdkHome(project)
        if (versionText != null) {
            return FlutterLocalVersion.VersionString(versionText)
        }
        val versionInfo = readVersionFromCacheVersionJsonFile(project)
        if (versionInfo != null) {
            return FlutterLocalVersion.VersionInfo(versionInfo)
        }
        return null
    }

    fun buildChangeLogWebUrl(version: String): String {
        return "https://github.com/flutter/flutter/blob/${version}/CHANGELOG.md#${version.replace(".","")}"
    }

}