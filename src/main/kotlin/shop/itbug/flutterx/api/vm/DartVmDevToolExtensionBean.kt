package shop.itbug.flutterx.api.vm

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import shop.itbug.flutterx.i18n.PluginBundle
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

class DartVmDevToolExtensionBean : BaseKeyedLazyInstance<DartVmDevToolExtension>() {
    @Attribute("id")
    @RequiredElement
    lateinit var id: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    @Attribute("descriptionKey")
    var descriptionKey: String? = null

    override fun getImplementationClassName(): String = implementation

    fun createExtension(): DartVmDevToolExtension = getInstance()

    fun getDescription(): String? {
        val key = descriptionKey?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (pluginDescriptor.pluginId.idString == FLUTTERX_PLUGIN_ID) {
            resolveFlutterXMessage(key)
        } else {
            resolveContributorMessage(key)
        }
    }

    private fun resolveFlutterXMessage(key: String): String? {
        return try {
            PluginBundle.get(key)
        } catch (_: MissingResourceException) {
            null
        }
    }

    private fun resolveContributorMessage(key: String): String? {
        val bundleName = pluginDescriptor.resourceBundleBaseName ?: return null
        return try {
            ResourceBundle.getBundle(bundleName, Locale.getDefault(), loaderForClass).getString(key)
        } catch (_: MissingResourceException) {
            null
        }
    }

    companion object {
        val EP_NAME: ExtensionPointName<DartVmDevToolExtensionBean> =
            ExtensionPointName.create("shop.itbug.FlutterCheckVersionX.dartVmDevTool")

        private const val FLUTTERX_PLUGIN_ID = "shop.itbug.FlutterCheckVersionX"
    }
}
