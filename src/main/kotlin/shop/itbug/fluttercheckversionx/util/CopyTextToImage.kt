package shop.itbug.fluttercheckversionx.util

import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.json.JsonFileType
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.ImageUtil
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.IOException
import kotlin.math.max


data class CopyImageConfig(var useShadow: Boolean = false, var shadowSize: Int = 20)

///文本转图像,并拷贝到剪贴板
object CopyImageToClipboard {


    private fun copyImageToClipboard(image: BufferedImage) {
        val transferableImage = TransferableImage(image)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(transferableImage, null)
    }

    fun saveStringAsImageToClipboard(text: String) {
        require(text.isNotEmpty()) { "Text cannot be null or empty." }


        // 按行分割文本
        val lines: List<String> = text.split("\n")


        // 创建一个临时的 BufferedImage 以便获取 FontMetrics
        val tempImage = ImageUtil.createImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2dTemp = tempImage.createGraphics()
        g2dTemp.font = getFont() // 使用等宽字体确保格式化的文本对齐


        // 获取最长行的宽度
        var textWidth = 0
        for (line in lines) {
            val lineWidth = g2dTemp.fontMetrics.stringWidth(line)
            if (lineWidth > textWidth) {
                textWidth = lineWidth
            }
        }


        // 获取行高
        val textHeight = g2dTemp.fontMetrics.height


        // 释放临时 Graphics 对象
        g2dTemp.dispose()


        // 确保宽度和高度为正数
        val imageWidth = max((textWidth + 20).toDouble(), 1.0).toInt()
        val imageHeight = max(((textHeight * lines.size) + 20).toDouble(), 1.0).toInt()


        // 根据文本大小创建合适大小的 BufferedImage
        val image = ImageUtil.createImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()


        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)


        // 设置背景颜色为白色，并填充整个图像
        g2d.color = JBColor.WHITE
        g2d.fillRect(0, 0, image.width, image.height)


        // 设置字体和颜色
        g2d.font = getFont()
        g2d.color = JBColor.BLACK


        // 逐行绘制字符串
        val x = 10 // X 坐标偏移
        var y = 10 + textHeight // Y 坐标偏移，10 用于上方留空
        for (line in lines) {
            g2d.drawString(line, x, y)
            y += textHeight // 移动到下一行
        }


        // 释放 Graphics 对象
        g2d.dispose()


        // 将图像复制到剪贴板
        copyImageToClipboard(image)
        println("拷贝完成..")
    }

    fun saveJsonStringAsImageToClipboard(project: Project, text: String, config: CopyImageConfig = CopyImageConfig()) {
        require(text.isNotEmpty()) { "Text cannot be null or empty." }
        val useShadow = config.useShadow
        val shadowSize = config.shadowSize
        val fileType = JsonFileType.INSTANCE
        val psiFile: PsiFile = PsiFileFactory.getInstance(project).createFileFromText("temp.json", fileType, text)
        val document = EditorFactory.getInstance().createDocument(psiFile.text)
        val editor = EditorFactory.getInstance().createEditor(document, project, fileType, true)
        if (editor is EditorEx) {
            editor.highlighter = HighlighterFactory.createHighlighter(project, "temp.json")
            editor.colorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme
            editor.settings.isLineNumbersShown = true
        }
        val editorTextField = JsonEditorTextPanel(project, text)
        editorTextField.addNotify() // 强制初始化编辑器组件
        editorTextField.font = getFont()
        val size = editorTextField.preferredSize
        var width = size.width
        var height = size.height
        if (useShadow) {
            width += shadowSize * 2
            height += shadowSize * 2
        }
        val image = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        if (useShadow) {
            g2d.color = Color.WHITE
            for (i in shadowSize downTo 1) {
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f * (shadowSize - i) / shadowSize)
                g2d.fillRoundRect(i, i, size.width + shadowSize, size.height + shadowSize, shadowSize, shadowSize)
            }
        }
        g2d.font = getFont()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.color = JBColor.WHITE
        g2d.fillRect(0, 0, size.width, size.height)
        if (useShadow) {
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
            editor.contentComponent.size = size
            editor.contentComponent.paint(g2d.create(shadowSize, shadowSize, size.width, size.height))
        } else {
            editor.contentComponent.paint(g2d)
        }
        g2d.dispose()
        EditorFactory.getInstance().releaseEditor(editor)
        copyImageToClipboard(image)
    }

    private fun getFont(): Font {
        return EditorColorsManager.getInstance().schemeForCurrentUITheme.getFont(EditorFontType.PLAIN)
    }

    // 定义一个 TransferableImage 类，用于将图像传输到剪贴板
    private class TransferableImage(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return DataFlavor.imageFlavor.equals(flavor)
        }

        @Throws(UnsupportedFlavorException::class, IOException::class)
        override fun getTransferData(flavor: DataFlavor): Any {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }
            return image
        }
    }
}