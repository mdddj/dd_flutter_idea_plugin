package shop.itbug.fluttercheckversionx.inlay

import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlay
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlayModel
import shop.itbug.fluttercheckversionx.tools.LibTools

class DartLibraryInlay : MyBaseInlay("Dart Library Tips") {
    override fun needHandle(element: PsiElement, setting: PluginSetting): Boolean {
        return element.text.trim().contains("part of")
    }

    override fun handle(element: PsiElement, myFactory: HintsInlayPresentationFactory, model: MyBaseInlayModel) {
        val libs = LibTools.getLibraryFiles(element.project).joinToString(
            separator = ",", postfix = "."
        )
        model.sink.addBlockElement(
            element.startOffset,
            relatesToPrecedingText = true,
            showAbove = true,
            priority = 1,
            presentation = myFactory.simpleText(
                libs,
                null
            ) { _, _ ->
                run {

                }
            }
        )
    }
}