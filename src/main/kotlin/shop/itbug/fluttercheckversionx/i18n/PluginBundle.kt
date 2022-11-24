package shop.itbug.fluttercheckversionx.i18n

const val pathToBundleKey = "messages.pluginBundle"

object  PluginBundle: DynamicPluginBundle(pathToBundleKey) {

    fun get(key: String, vararg params: Any) : String {
        return getMessage(key, params)
    }

}