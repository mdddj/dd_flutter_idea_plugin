package shop.itbug.fluttercheckversionx.inlay.freezed

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.fluttercheckversionx.manager.myManagerFun

/**
 * freezed相关操作
 */
class FreezedActionInlayProvider : InlayHintsProvider {
    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return object : SharedBypassCollector {
            override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
                if (element.canShowActionInlays()) {

                }
            }
        }
    }

    //是否能显示freezed相关操作
    private fun PsiElement.canShowActionInlays(): Boolean {
        return this is DartClassDefinitionImpl && this.myManagerFun().hasFreezeMetadata()
    }
}