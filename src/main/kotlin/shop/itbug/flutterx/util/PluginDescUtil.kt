package shop.itbug.flutterx.util

import codegen.FlutterXPluginInfo
import com.intellij.openapi.application.PathManager
import org.jetbrains.kotlin.konan.file.File

object PluginDescUtil {
    fun getPluginName(): String = FlutterXPluginInfo.NAME

    fun getPluginFontsDir(): String {
        val path = PathManager.getPluginsPath() + File.separator + getPluginName() + File.separator + "fonts"
        val file = File(path)
        if (file.exists.not()) {
            file.mkdirs()
        }
        return path
    }
}