package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

object DartPubTools {

    /**
     * 读取pub cache 目录
     */
    suspend fun getPubCacheDir(): VirtualFile? {
        val pubCache = System.getenv("PUB_CACHE")
        val pubCacheDir = if (pubCache.isNullOrEmpty()) {
            if (SystemInfo.isWindows) {
                "${System.getenv("APPDATA")}\\Pub\\Cache"
            } else {
                "${System.getProperty("user.home")}/.pub-cache"
            }
        } else {
            pubCache
        }
        return readAction { LocalFileSystem.getInstance().findFileByPath(pubCacheDir) }
    }
}