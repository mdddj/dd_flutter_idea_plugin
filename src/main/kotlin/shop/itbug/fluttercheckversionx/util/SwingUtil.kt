package shop.itbug.fluttercheckversionx.util

import java.awt.*
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon


object SwingUtil {
    /**创建一个可以自适应组件大小的ImageIcon对象
     * @param image 从` Image `对象来创建ImageIcon
     * @param constrained 是否等比例缩放 。当为` true `时，可通过
     * [javax.swing.JComponent.setAlignmentX]和
     * [javax.swing.JComponent.setAlignmentY]方法设置组件对齐方式。
     * @date  2019-08-20
     */
    private fun createAutoAdjustIcon(image: Image?, constrained: Boolean): ImageIcon {
        return object : ImageIcon(image) {
            @Synchronized
            override fun paintIcon(cmp: Component, g: Graphics, x: Int, y: Int) {
                //初始化参数
                val startPoint = Point(0, 0) //默认绘制起点
                val cmpSize = cmp.size //获取组件大小
                var imgSize = Dimension(iconWidth, iconHeight) //获取图像大小

                //计算绘制起点和区域
                if (constrained) { //等比例缩放
                    //计算图像宽高比例
                    val ratio = 1.0 * imgSize.width / imgSize.height
                    //计算等比例缩放后的区域大小
                    imgSize.width = cmpSize.width.toDouble().coerceAtMost(ratio * cmpSize.height).toInt()
                    imgSize.height = (imgSize.width / ratio).toInt()
                    //计算绘制起点
                    startPoint.x = (cmp.alignmentX * (cmpSize.width - imgSize.width)).toInt()
                    startPoint.y = (cmp.alignmentY * (cmpSize.height - imgSize.height)).toInt()
                } else { //完全填充
                    imgSize = cmpSize
                }

                //根据起点和区域大小进行绘制
                if (imageObserver == null) {
                    g.drawImage(
                        getImage(), startPoint.x, startPoint.y,
                        imgSize.width, imgSize.height, cmp
                    )
                } else {
                    g.drawImage(
                        getImage(), startPoint.x, startPoint.y,
                        imgSize.width, imgSize.height, imageObserver
                    )
                }
            }
        }
    }


    fun createAutoAdjustIconWithMyIcon(icon: Icon, constrained: Boolean = true): ImageIcon {
        return createAutoAdjustIcon(iconToImage(icon), constrained)
    }

    private fun iconToImage(icon: Icon): Image? {
        return if (icon is ImageIcon) {
            icon.image
        } else {
            val w = icon.iconWidth
            val h = icon.iconHeight
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val gd = ge.defaultScreenDevice
            val gc = gd.defaultConfiguration
            val image = gc.createCompatibleImage(w, h)
            val g = image.createGraphics()
            icon.paintIcon(null, g, 0, 0)
            g.dispose()
            image
        }
    }

    /**
     * 文件转icon
     */
    fun fileToIcon(file: File): Icon? {
        try {
            val buf = ImageIO.read(file)
            return ImageIcon(buf)
        } catch (e: Exception) {
            return null
        }
    }
}
