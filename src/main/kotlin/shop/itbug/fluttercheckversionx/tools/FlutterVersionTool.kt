package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.lang.dart.sdk.DartSdk
import java.io.IOException
import java.nio.charset.StandardCharsets


data class MyFlutterVersion(val version: String?)

object FlutterVersionTool {

    private const val DART_SDK_SUFFIX = "/bin/cache/dart-sdk"

    fun readVersionFromSdkHome(project: Project): MyFlutterVersion? {
        val dartSdkHome = DartSdk.getDartSdk(project) ?: return null
        val dartPath = dartSdkHome.homePath
        if (dartPath.endsWith(DART_SDK_SUFFIX).not()) {
            return null
        }
        val sdkPath: String = dartPath.substring(0, dartPath.length - DART_SDK_SUFFIX.length)
        val home = LocalFileSystem.getInstance().findFileByPath(sdkPath) ?: return null
        val versionFile = home.findChild("version") ?: return null
        val versionText = readVersionString(versionFile) ?: return null
        return MyFlutterVersion(version = versionText)
    }

    ///在flutter根目录下读取version版本号
    private fun readVersionString(file: VirtualFile): String? {
        try {
            val data = String(file.contentsToByteArray(), StandardCharsets.UTF_8)
            for (line in data.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val t = line.trim { it <= ' ' }
                if (t.isEmpty() || t.startsWith("#")) {
                    continue
                }
                return line
            }
            return null
        } catch (e: IOException) {
            return null
        }
    }
}