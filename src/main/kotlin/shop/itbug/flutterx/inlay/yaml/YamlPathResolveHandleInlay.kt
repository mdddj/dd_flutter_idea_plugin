package shop.itbug.flutterx.inlay.yaml

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.awt.RelativePoint
import shop.itbug.flutterx.api.inlay.PubspecInlayContext
import shop.itbug.flutterx.api.inlay.PubspecInlayProvider
import shop.itbug.flutterx.icons.MyIcons
import java.awt.Cursor
import java.awt.event.MouseEvent
import java.io.File

class YamlPathResolveHandleInlay : PubspecInlayProvider {
    override fun collect(context: PubspecInlayContext) {
        val packageContext = context.packageContext ?: return
        val findFile = packageContext.yamlExtends.getPathResolvePath() ?: return
        val factory = context.factory

        context.addInlineElement(
            presentation = factory.inset(
                factory.roundWithBackground(
                    factory.onClick(
                        factory.withCursorOnHover(
                            factory.withTooltip(
                                "Open in ...",
                                factory.smallText(findFile.path),
                            ),
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
                        ),
                        MouseButton.Left,
                    ) { event, _ ->
                        showOpenInPopup(event, context, findFile)
                    }
                ),
                left = 5,
            ),
        )

        context.addInlineElement(
            presentation = factory.inset(
                factory.roundWithBackground(
                    factory.onClick(
                        factory.withCursorOnHover(
                            factory.smallScaledIcon(MyIcons.moreHorizontal),
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
                        ),
                        MouseButton.Left,
                    ) { event, _ ->
                        showOpenInPopup(event, context, findFile)
                    }
                ),
                left = 5,
            ),
        )
    }

    private fun showOpenInPopup(
        event: MouseEvent,
        context: PubspecInlayContext,
        file: File,
    ) {
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return
        val originalActionGroup =
            ActionManager.getInstance().getAction("FlutterXOpenInAction") as? DefaultActionGroup ?: return
        val projectContext = SimpleDataContext.getProjectContext(context.file.project)
        val dataContext = SimpleDataContext.getSimpleContext(
            CommonDataKeys.VIRTUAL_FILE,
            virtualFile,
            projectContext,
        )
        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Open in ...",
            originalActionGroup,
            dataContext,
            JBPopupFactory.ActionSelectionAid.MNEMONICS,
            true,
        )
        popup.show(RelativePoint.fromScreen(event.locationOnScreen))
    }
}
