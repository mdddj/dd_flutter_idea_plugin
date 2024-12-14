package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.lang.dart.psi.impl.DartFactoryConstructorDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartNamedConstructorDeclarationImpl
import shop.itbug.fluttercheckversionx.inlay.getLine


/**
 * 统计使用次数
 */
class DartUseageCountInlay : InlayHintsProvider {

    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): InlayHintsCollector? {

        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {


                // todo 使用位置
                return
                when (element) {
                    is DartNamedConstructorDeclarationImpl -> {
                        element.componentName?.let { viewInEnd(it, sink, editor) }
                    }

                    is DartFactoryConstructorDeclarationImpl -> {
                        element.componentName?.let { viewInEnd(it, sink, editor) }
                    }
                }
            }

            fun viewInEnd(element: PsiElement, sink: InlayTreeSink, editor: Editor) {
                val size =
                    ReferencesSearch.search(element, GlobalSearchScope.projectScope(element.project)).findAll().size
                println("引用次数: $size")
                sink.addPresentation(EndOfLinePosition(editor.getLine(element)), null, null, HintFormat.default) {
                    this.text("$size useage")
                }
            }

        }
    }
}