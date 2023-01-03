package shop.itbug.fluttercheckversionx.i18n

import com.intellij.AbstractBundle
import com.intellij.lang.LangBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.services.PluginStateService
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.*

///抽象类，支持加载与安装的语言包匹配的本地化消息。
open class DynamicPluginBundle(pathToBundle: String): AbstractBundle(pathToBundle) {
    override fun findBundle(
        pathToBundle: String,
        loader: ClassLoader,
        control: ResourceBundle.Control
    ): ResourceBundle {
        val base = super.findBundle(pathToBundle, loader, control)
        val ideLocale = LangBundle.getLocale()
        val setting =  PluginStateService.getInstance().state ?: AppStateModel()
        if(setting.lang != "中文"){
            val localizedPath = pathToBundle + "_"+ideLocale.language
            val localBundle = super.findBundle(localizedPath, DynamicPluginBundle::class.java.classLoader, control)
            if(base != localBundle){
                setParent(localBundle,base)
                return localBundle
            }
        }
        return base
    }
    /**
     * 从com.intellij中借用代码。DynamicBundle使用反射设置父捆绑包。
     */
    private fun setParent(localeBundle: ResourceBundle, base: ResourceBundle) {
        try {
            val method: Method = ResourceBundle::class.java.getDeclaredMethod("setParent", ResourceBundle::class.java)
            method.isAccessible = true
            MethodHandles.lookup().unreflect(method).bindTo(localeBundle).invoke(base)
        } catch (e: Throwable) {
            // ignored, better handle this in production code
        }
    }
}