package actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import services.PUBL_API_URL
import services.PubService
import services.await

///检测版本更新
class CheckFlutterPluginHasNewVersion: AnAction() {


    override fun actionPerformed(e: AnActionEvent) {
        val psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT)
        if (psiElement != null) {
            val text = psiElement.text
            if (text != null) {
                if (text.contains(": ^") || text.contains(": any")) {
                    val pluginName = text.split(":")[0];
                    val build = Retrofit.Builder().baseUrl(PUBL_API_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val pubService = build.create(PubService::class.java)
                   val response = pubService.callPluginDetails(pluginName).execute()
                    println(response.body())

                }
            }
        }
    }
}