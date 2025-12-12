package shop.itbug.flutterx.i18n

const val pathToBundleKey = "messages.pluginBundle"

/**
 * 国际化配置
 */
object PluginBundle : DynamicPluginBundle(pathToBundleKey) {

    fun get(key: String, vararg params: Any): String {
        return getMessage(key.trim(), params)
    }

    val doc: String get() = get("doc")

    fun defaultText() = get("default.value")
}

fun String.i18n(): String {
    return PluginBundle.get(this)
}