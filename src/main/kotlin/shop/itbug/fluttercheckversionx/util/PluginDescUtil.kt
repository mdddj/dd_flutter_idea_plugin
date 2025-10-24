package shop.itbug.fluttercheckversionx.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.kotlin.konan.file.File

object PluginDescUtil {
    fun getPluginId() = PluginId.getId("shop.itbug.FlutterCheckVersionX")
    fun getPluginName(): String = PluginManagerCore.getPlugin(getPluginId())!!.name

    fun getPluginFontsDir(): String {
        val path = PathManager.getPluginsPath() + File.separator + getPluginName() + File.separator + "fonts"
        val file = File(path)
        if (file.exists.not()) {
            file.mkdirs()
        }
        return path
    }
}