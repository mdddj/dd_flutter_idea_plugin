package shop.itbug.fluttercheckversionx.inlay.json

import com.intellij.codeInsight.hints.*
import com.intellij.json.psi.impl.JsonStringLiteralImpl
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class JsonImageInlay : InlayHintsProvider<JsonImageInlay.Setting> {

    data class Setting(private val enable: Boolean)

    override val key: SettingsKey<Setting>
        get() = SettingsKey("json image viewer")
    override val name: String
        get() = "json image viewer"
    override val previewText: String
        get() = ""

    override fun createSettings(): Setting {
        return Setting(true)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Setting,
        sink: InlayHintsSink
    ): InlayHintsCollector {

        return object : FactoryInlayHintsCollector(editor) {

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val hintsInlayPresentationFactory = HintsInlayPresentationFactory(factory)
                if (element is JsonStringLiteralImpl) {
                    val imageUrl = element.text.replace("\"", "")
                    val isAImageUrl = isImgUrl(imageUrl)
                    if (isAImageUrl) {
                        sink.addInlineElement(
                            element.endOffset,
                            false,
                            hintsInlayPresentationFactory.simpleText("图片", "鼠标移动到链接上面即可预览"),
                            false
                        )
                    }

                }

                return true
            }

        }

    }


    /**
     * 创建配置小部件,这里就不设置了
     */
    override fun createConfigurable(settings: Setting): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }


    private val imageUrlPattern: Pattern = Pattern
        .compile(".*?(gif|jpeg|png|jpg|bmp)")


    /**
     * 判断一个url是否为图片url
     *
     * @param url
     * @return
     */
    fun isImgUrl(url: String?): Boolean {
        return if (url == null || url.trim { it <= ' ' }.isEmpty()) false else imageUrlPattern.matcher(url).matches()
    }

}

class DefaulImmediateConfigurable: ImmediateConfigurable {
    override fun createComponent(listener: ChangeListener): JComponent {
        return  JLabel("nothing")
    }

}