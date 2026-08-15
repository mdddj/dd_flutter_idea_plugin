package shop.itbug.flutterx.inlay.yaml

import shop.itbug.flutterx.api.inlay.PubspecInlayContext
import shop.itbug.flutterx.api.inlay.PubspecInlayProvider

class PubspecPackageUpdateTimeShowInlay : PubspecInlayProvider {
    override fun collect(context: PubspecInlayContext) {
        val packageContext = context.packageContext ?: return
        val model = packageContext.dartYamlModel ?: return
        packageContext.pubVersionDataModel ?: return
        val factory = context.factory

        model.getLastUpdateTimeFormatString().takeIf { it.isNotBlank() }?.let { lastUpdate ->
            context.addInlineElement(
                presentation = factory.inset(
                    factory.smallTextWithoutBackground(lastUpdate),
                    left = 5,
                ),
                placeAtTheEndOfLine = true,
            )
        }

        model.getLastVersionText()?.let { lastVersion ->
            context.addInlineElement(
                presentation = factory.inset(
                    factory.roundWithBackground(factory.smallText(lastVersion)),
                    left = 5,
                ),
                relatesToPrecedingText = true,
            )
        }
    }
}
