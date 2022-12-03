package shop.itbug.fluttercheckversionx.widget

import com.intellij.ui.JBColor
import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.net.URL
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthIcon

/**
 * 异步绘制网络图片
 */
class AvatarIcon(private val  height: Int, private val width : Int, var url: String) : SynthIcon {
    override fun paintIcon(context: SynthContext?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
        val image = Toolkit.getDefaultToolkit().getImage(URL(url))
        g?.apply {
            val g2 = g as Graphics2D
            g2.composite = AlphaComposite.Src
            g2.color = JBColor.white
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
            g2.fill(RoundRectangle2D.Float(0F, 0F, width.toFloat(), height.toFloat(), 100F, 100F))
            g2.composite = AlphaComposite.SrcIn
            g2.drawImage(image,x,y,width,height,null)
        }

    }

    override fun getIconWidth(context: SynthContext?): Int {
        return width
    }

    override fun getIconHeight(context: SynthContext?): Int {
        return height
    }


}