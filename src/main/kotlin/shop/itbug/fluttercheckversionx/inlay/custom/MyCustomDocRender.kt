package shop.itbug.fluttercheckversionx.inlay.custom

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.text.CharArrayUtil
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.JComponent
import javax.swing.JEditorPane

class MyCustomDocRender(private val docString: String?,private val editor: Editor) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return calcWidth(inlay.editor)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
        val startX = calcInlayStartX(TextRange(inlay.offset,inlay.bounds?.width?:0))
        val endX = r.x + r.width
        println("startx = $startX endx = $endX")
        if(startX>= endX) return
        val margin = scale(4)
        val filledHeight: Int = r.height - margin * 2
        if (filledHeight <= 0) return
        val filledStartY: Int = r.y + margin
        val defaultBgColor = (editor as EditorEx).backgroundColor
        val currentBgColor = textAttributes.backgroundColor
        val bgColor = if (currentBgColor == null) defaultBgColor else ColorUtil.mix(
            defaultBgColor,
            textAttributes.backgroundColor,
            .5
        )
        if (currentBgColor != null) {
            g.color = Color.red
            val arcDiameter = 5 * 2
            if (endX - startX >= arcDiameter) {
                g.fillRect(startX, filledStartY, endX - startX - 5, filledHeight)
                val savedHint = (g as Graphics2D).getRenderingHint(RenderingHints.KEY_ANTIALIASING)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.fillRoundRect(endX - arcDiameter, filledStartY, arcDiameter, filledHeight, arcDiameter, arcDiameter)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedHint)
            } else {
                g.fillRect(startX, filledStartY, endX - startX, filledHeight)
            }
        }
        g.color = editor.getColorsScheme().getColor(DefaultLanguageHighlighterColors.DOC_COMMENT_GUIDE)
        g.fillRect(startX, filledStartY, scale(2), filledHeight)
        val topBottomInset = scale(2)
        val componentWidth =
            endX - startX - scale(14) - scale(12)
        val componentHeight = filledHeight - topBottomInset * 2
        if (componentWidth > 0 && componentHeight > 0) {
            val component: JComponent = MyEditPanel()
            component.background = bgColor
            val dg = g.create(
                startX + scale(14),
                filledStartY + topBottomInset,
                componentWidth,
                componentHeight
            )
            UISettings.setupAntialiasing(dg)
            component.paintComponents(dg)
            dg.dispose()
        }
    }





    private fun calcInlayStartX(textRange: TextRange): Int {
        val highlighter = editor.markupModel.addRangeHighlighter(
            null,
            textRange.startOffset,
            textRange.endOffset,
            0,
            HighlighterTargetArea.EXACT_RANGE
        )
        if (highlighter.isValid) {
            val document = editor.document
            val nextLineNumber = document.getLineNumber(highlighter.endOffset) + 1
            if (nextLineNumber < document.lineCount) {
                val lineStartOffset = document.getLineStartOffset(nextLineNumber)
                val contentStartOffset =
                    CharArrayUtil.shiftForward(document.immutableCharSequence, lineStartOffset, " \t\n")
                return editor.offsetToXY(contentStartOffset, false, true).x
            }
        }
        return editor.insets.left
    }

    private fun calcWidth(editor: Editor): Int {
        val availableWidth = editor.scrollingModel.visibleArea.width
        return if (availableWidth <= 0) {
            MAX_WIDTH
        } else scale(MIN_WIDTH).coerceAtLeast(scale(MAX_WIDTH).coerceAtMost(availableWidth))
    }

    companion object {
        const val MAX_WIDTH = 680
        const val MIN_WIDTH = 350
        fun scale(value: Int): Int {
            return (value * UISettings.defFontScale).toInt()
        }
    }

}


class MyEditPanel : JEditorPane() {

    init {
        val label = JBLabel("梁典典")
        label.foreground = JBUI.CurrentTheme.Label.foreground()
        add(label)
    }
}
