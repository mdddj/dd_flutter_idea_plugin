package shop.itbug.flutterx.inlay.yaml

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.psi.util.endOffset
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.flutterx.api.inlay.PubspecInlayContext
import shop.itbug.flutterx.api.inlay.PubspecInlayProvider
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.MyActionUtil
import java.awt.Cursor

class DartPackageSearchDialogInlay : PubspecInlayProvider {
    override fun collect(context: PubspecInlayContext) {
        val element = context.element
        val dependenciesElement = element.parent as? YAMLKeyValueImpl ?: return
        if (dependenciesElement.key !== element || dependenciesElement.keyText != "dependencies") return

        val factory = context.factory
        context.addInlineElement(
            offset = element.endOffset,
            presentation = factory.onClick(
                factory.withCursorOnHover(
                    factory.roundWithBackground(
                        factory.seq(
                            factory.smallScaledIcon(MyIcons.dartPackageIcon),
                            factory.smallText(" Add package"),
                        )
                    ),
                    Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
                ),
                MouseButton.Left,
            ) { _, _ ->
                MyActionUtil.showPubSearchDialog(
                    project = element.project,
                    context.file,
                )
            },
            placeAtTheEndOfLine = true,
        )
    }
}
