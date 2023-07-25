package shop.itbug.fluttercheckversionx.i18n

import com.intellij.AbstractBundle
import shop.itbug.fluttercheckversionx.services.PluginStateService
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.util.*

///抽象类，支持加载与安装的语言包匹配的本地化消息。
open class DynamicPluginBundle(pathToBundle: String) : AbstractBundle(pathToBundle) {
    override fun findBundle(
        pathToBundle: String,
        loader: ClassLoader,
        control: ResourceBundle.Control
    ): ResourceBundle {
        val base = super.findBundle(pathToBundle, loader, control)
        val ideLocale = Locale.getDefault().language
        val settingLang = PluginStateService.getInstance().state?.lang ?: ""
        val localizedPath = when (settingLang) {
            "English" -> {
                pathToBundle + "_en"
            }

            "繁體" -> {
                pathToBundle + "_hk"
            }

            "中文" -> {
                pathToBundle
            }

            "한국어" -> {
                pathToBundle + "_ko"
            }

            "日本語" -> {
                pathToBundle + "_ja"
            }

            else -> {
                when (ideLocale) {
                    "en" -> pathToBundle + "_en"
                    "ja" -> pathToBundle + "_ja"
                    "hk" -> pathToBundle + "_hk"
                    "ko" -> pathToBundle + "_ko"
                    else -> {
                        pathToBundle
                    }
                }
            }
        }

        val localBundle = super.findBundle(localizedPath, DynamicPluginBundle::class.java.classLoader, control)
        if (base != localBundle) {
            setParent(localBundle, base)
            return localBundle
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