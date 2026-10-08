package shop.itbug.flutterx.inlay.yaml

import shop.itbug.flutterx.api.inlay.PubspecInlayContext
import shop.itbug.flutterx.api.inlay.PubspecInlayProvider
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.util.BuiltInKotlinCheckResult
import shop.itbug.flutterx.util.BuiltInKotlinCheckUtil
import shop.itbug.flutterx.util.BuiltInKotlinStatus

/** Shows whether an installed package can be used with AGP 9 built-in Kotlin. */
class BuiltInKotlinCompatibilityInlay : PubspecInlayProvider {
    override fun collect(context: PubspecInlayContext) {
        val packageContext = context.packageContext ?: return
        val result = BuiltInKotlinCheckUtil.checkPackageBuiltInKotlinSupport(
            context.file.project,
            packageContext.name,
        ) ?: return

        val presentation = context.factory.inset(
            context.factory.roundWithBackground(
                context.factory.smallText(result.label()),
            ),
            left = 5,
        )
        context.addInlineElement(
            presentation = presentation,
            relatesToPrecedingText = true,
            placeAtTheEndOfLine = true,
        )
    }
}

private fun BuiltInKotlinCheckResult.label(): String = when (status) {
    BuiltInKotlinStatus.MIGRATED -> PluginBundle.get("built.in.kotlin.status.migrated")
    BuiltInKotlinStatus.COMPATIBLE -> PluginBundle.get("built.in.kotlin.status.compatible")
    BuiltInKotlinStatus.UNMIGRATED -> PluginBundle.get("built.in.kotlin.status.unmigrated")
    BuiltInKotlinStatus.NO_KOTLIN -> PluginBundle.get("built.in.kotlin.status.no.kotlin")
}
