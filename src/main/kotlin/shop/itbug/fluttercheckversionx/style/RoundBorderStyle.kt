package shop.itbug.fluttercheckversionx.style

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.border.Border

class RoundBorderStyle(private val color: Color) : Border {
    constructor() : this(JBColor.border())

    override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
        g?.color = color
        g?.drawRoundRect(0, 0, c?.width?.minus(1) ?: width, c?.height?.minus(1) ?: height, 15, 15)
    }

    override fun getBorderInsets(c: Component?): Insets {
        return JBUI.insets(12)
    }

    override fun isBorderOpaque(): Boolean {
        return true
    }
}