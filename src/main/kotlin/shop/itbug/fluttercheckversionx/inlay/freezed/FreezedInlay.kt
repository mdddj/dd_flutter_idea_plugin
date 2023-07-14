package shop.itbug.fluttercheckversionx.inlay.freezed

import com.intellij.codeInsight.hints.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.getLineStart
import shop.itbug.fluttercheckversionx.util.dart.DartClassUtil
import javax.swing.JComponent


data class FreezedInlaySetting(var show: Boolean)

class FreezedInlay : InlayHintsProvider<FreezedInlaySetting> {
    override val key: SettingsKey<FreezedInlaySetting>
        get() = SettingsKey("freezed inlay")
    override val name: String
        get() = "FreezedInlay"
    override val previewText: String
        get() = "@freezed" +
                "class Test {}"

    override fun createSettings(): FreezedInlaySetting {
        return FreezedInlaySetting(true)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: FreezedInlaySetting,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return FreezedInlayCollector(editor)
    }

    override fun createConfigurable(settings: FreezedInlaySetting): ImmediateConfigurable {
        return FreezedInlayPanel()
    }
}


class FreezedInlayCollector(edit:Editor) : FactoryInlayHintsCollector(edit) {

    val inlayFactory = HintsInlayPresentationFactory(factory)

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        val isFreezedClass = DartClassUtil.hasMetadata(element,"freezed") || DartClassUtil.hasMetadata(element,"Freezed")
        if(isFreezedClass){
            val lineStart = editor.getLineStart(element)
            val inlayPresentation = inlayFactory.iconText(AllIcons.Actions.Properties,"Freezed class actions")
            sink.addBlockElement(lineStart,true,true,0,inlayPresentation)
        }
        return true
    }
}

class FreezedInlayPanel : ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        return panel {  }
    }

}