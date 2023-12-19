package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlay
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlayModel
import java.awt.event.MouseEvent

class PsiElementEditorPopupMenuInlay : MyBaseInlay("WidgetCovertToRiverpod") {
    override fun needHandle(element: PsiElement, setting: PluginSetting): Boolean = element is DartClassDefinitionImpl

    override fun handle(element: PsiElement, myFactory: HintsInlayPresentationFactory, model: MyBaseInlayModel) {
        model.sink.addBlockElement(
            element.startOffset,
            true,
            showAbove = true,
            priority = 1,
            presentation = myFactory.iconText(
                MyIcons.apiIcon, "Riverpod", true,
                handle = { mouseEvent, _ ->
                    run {
                        showPopup(mouseEvent)
                    }
                },
            )
        )
    }

    private fun showPopup(mouseEvent: MouseEvent) {
        val group = ActionManager.getInstance().getAction("WidgetToRiverpod") as DefaultActionGroup
        val popupCreate = JBPopupFactory.getInstance().createActionGroupPopup(
            "Riverpod To", group, DataContext.EMPTY_CONTEXT,
            JBPopupFactory.ActionSelectionAid.MNEMONICS, true
        )
        popupCreate.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen))
    }
}