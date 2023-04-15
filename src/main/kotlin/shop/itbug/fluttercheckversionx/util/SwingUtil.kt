package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.lang.dart.DartLanguage
import org.intellij.plugins.markdown.lang.MarkdownFileType
import org.intellij.plugins.markdown.ui.preview.MarkdownEditorWithPreview
import org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditorProvider
import java.awt.*
import java.net.URL
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
                    imgSize.width = Math.min(cmpSize.width.toDouble(), ratio * cmpSize.height).toInt()
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

    /**创建一个可以自适应组件大小的Icon对象
     * @param filename 指定文件名或者路径的字符串
     * @param constrained 是否等比例缩放。当为` true `时，可通过
     * [javax.swing.JComponent.setAlignmentX]和
     * [javax.swing.JComponent.setAlignmentY]方法设置组件对齐方式。
     * @date  2019-08-20
     */
    fun createAutoAdjustIcon(filename: String?, constrained: Boolean): ImageIcon {
        return createAutoAdjustIcon(ImageIcon(filename).image, constrained)
    }

    /**创建一个可以自适应组件大小的ImageIcon对象
     * @param url 从指定的` URL `对象来创建ImageIcon
     * @param constrained 是否等比例缩放 。当为` true `时，可通过
     * [javax.swing.JComponent.setAlignmentX]和
     * [javax.swing.JComponent.setAlignmentY]方法设置组件对齐方式。
     * @date  2019-08-20
     */
    fun createAutoAdjustIcon(url: URL?, constrained: Boolean): ImageIcon {
        return createAutoAdjustIcon(ImageIcon(url).image, constrained)
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

    fun getMkEditor(project: Project, initText: String = ""): MarkdownEditorWithPreview {
        val vF = LightVirtualFile("D", MarkdownFileType.INSTANCE, initText)
        val mkEdit =
            FileEditorProviderManager.getInstance().getProviders(project, vF).first() as MarkdownSplitEditorProvider
        val edit = mkEdit.createEditor(project, vF)
        return edit as MarkdownEditorWithPreview
    }

    fun getDartEditor(project: Project,initText: String="")  : PsiAwareTextEditorImpl {
        val vF = LightVirtualFile("freezed.dart", DartLanguage.INSTANCE, initText)
        val mkEdit =
            FileEditorProviderManager.getInstance().getProviders(project, vF).first()
        val edit = mkEdit.createEditor(project, vF)
        return edit as PsiAwareTextEditorImpl
    }
}
