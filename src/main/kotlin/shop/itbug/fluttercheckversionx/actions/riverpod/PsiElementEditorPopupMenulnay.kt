package shop.itbug.fluttercheckversionx.actions.riverpod

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.constance.MyKeys
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlay
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlayModel
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.event.MouseEvent


class PsiElementEditorPopupMenuInlay : MyBaseInlay("WidgetCovertToRiverpod") {


    override fun needHandle(element: PsiElement, setting: PluginSetting): Boolean {
        val config: List<String> = runBlocking { MyPsiElementUtil.getAllPlugins(element.project) }
        return setting.showRiverpodInlay && element is DartClassDefinitionImpl && config.contains("hooks_riverpod") && (element.superclass?.type?.text == "StatelessWidget" || element.superclass?.type?.text == "StatefulWidget")
    }

    override fun handle(element: PsiElement, myFactory: HintsInlayPresentationFactory, model: MyBaseInlayModel) {

        model.sink.addBlockElement(
            element.startOffset,
            true,
            showAbove = true,
            priority = 1,
            presentation = myFactory.iconText(
                AllIcons.General.ChevronDown, "Riverpod Tool", false,
                handle = { mouseEvent, _ ->
                    run {
                        showPopup(mouseEvent, model, element)
                    }
                },
            )
        )

    }


    ///create popup
    private fun showPopup(mouseEvent: MouseEvent, model: MyBaseInlayModel, element: PsiElement) {
        val group = ActionManager.getInstance().getAction("WidgetToRiverpod") as DefaultActionGroup
        model.editor.putUserData(MyKeys.DartClassKey, element as DartClassDefinitionImpl)
        val context = DataManager.getInstance().getDataContext(model.editor.component)
        val popupCreate = JBPopupFactory.getInstance().createActionGroupPopup(
            "Riverpod To", group, context,
            JBPopupFactory.ActionSelectionAid.MNEMONICS, true
        )
        popupCreate.show(RelativePoint.fromScreen(mouseEvent.locationOnScreen))
    }
}