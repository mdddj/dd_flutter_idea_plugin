package shop.itbug.fluttercheckversionx.i18n

const val pathToBundleKey = "messages.pluginBundle"

/**
 * 国际化配置
 */
object PluginBundle : DynamicPluginBundle(pathToBundleKey) {

    fun get(key: String, vararg params: Any): String {
        return getMessage(key.trim(), params)
    }
}

fun String.i18n(): String {
    return PluginBundle.get(this)
}