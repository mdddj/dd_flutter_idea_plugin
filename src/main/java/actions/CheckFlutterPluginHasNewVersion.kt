package actions

import com.google.common.cache.CacheLoader
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import common.YamlFileParser
import model.PluginVersion
import notif.NotifUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import services.PUBL_API_URL
import services.PubService
import services.await
import util.CacheUtil

///检测版本更新, 单个插件
class CheckFlutterPluginHasNewVersion: AnAction() {


    override fun actionPerformed(e: AnActionEvent) {
        val psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT)

    }
}