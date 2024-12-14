package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.codeInsight.hints.presentation.InputHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.awt.Graphics
import java.awt.Rectangle


private val doBuilderClasses = listOf<String>(

    //freezed
    "freezed", "Freezed",

    //copy with
    "CopyWith",

    //riverpod
    "riverpod", "Riverpod",

    //
    "JsonSerializable",

    //injectable
    "InjectableInit", "injectable", "singleton",

    //isar
    "collection", "Collection",

    //hive
    "HiveType",

    //autoequal_gen
    "autoequal"
)

/**
 * TODO
 * 在特殊的类后面添加run dart build command命令快捷方式
 * 检测为dart class
 * 读取类注解是否包含要build
 */
class DartRunBuildInlay : InlayHintsProvider {
    override fun createCollector(
        file: PsiFile, editor: Editor
    ): InlayHintsCollector? {


        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
            }


        }

    }


}


class Render : DartRunBuilderRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return 100
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        g.drawString("run build", targetRegion.x, targetRegion.y)
        super.paint(inlay, g, targetRegion, textAttributes)
    }
}


private interface DartRunBuilderRenderer : EditorCustomElementRenderer, InputHandler {

}