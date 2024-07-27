package shop.itbug.fluttercheckversionx.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.lang.dart.psi.impl.DartPartOfStatementImpl
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlay
import shop.itbug.fluttercheckversionx.inlay.base.MyBaseInlayModel
import shop.itbug.fluttercheckversionx.services.UserDartLibService

class DartLibraryInlay : MyBaseInlay("DartLibraryTips") {

    override fun needHandle(element: PsiElement, setting: PluginSetting, editor: Editor): Boolean {
        val text = editor.document.getText(element.textRange)
        return element is DartPartOfStatementImpl && text.trim()
            .startsWith("part of ") && UserDartLibService.getInstance(element.project).getLibraryNames()
            .isNotEmpty()
    }

    override fun handle(element: PsiElement, myFactory: HintsInlayPresentationFactory, model: MyBaseInlayModel) {
        val libs = UserDartLibService.getInstance(element.project).getLibraryNames().joinToString()
        model.sink.addBlockElement(element.startOffset,
            relatesToPrecedingText = true,
            showAbove = true,
            priority = 1,
            presentation = myFactory.simpleText(
                "${PluginBundle.get("find_the_project_dart_lib_names")}:  $libs", null
            ) { _, _ ->
                run {

                }
            })
    }

    override val previewText: String
        get() = """
            part of test;
        """.trimIndent()

}