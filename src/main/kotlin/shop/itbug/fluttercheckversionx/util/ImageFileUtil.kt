package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.images.index.ImageInfoIndex
import java.awt.Dimension
import java.awt.Image
import java.util.*
import javax.imageio.ImageIO
import javax.swing.ImageIcon


object ImageFileUtil {
    val IMAGE_EXTENSIONS: MutableSet<String?> = mutableSetOf<String?>(
        "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp", "ico", "tiff", "tif"
    )

    /**
     * 判断文件是不是图片
     */
    fun isImageFile(file: VirtualFile?): Boolean {
        if (file == null || file.isDirectory) {
            return false
        }
        val extension = file.getExtension()
        // 检查扩展名是否在图片扩展名列表中
        return extension != null && IMAGE_EXTENSIONS.contains(extension.lowercase(Locale.getDefault()))
    }


    fun getIcon(file: VirtualFile): ImageIcon {
        return ImageIcon(ImageIO.read(file.inputStream))
    }

    fun getSize(file: VirtualFile, project: Project): Dimension {
        val imageInfo = ImageInfoIndex.getInfo(file, project) ?: return Dimension()
        val width = imageInfo.width
        val height = imageInfo.height
        return Dimension(width, height)
    }

    fun resizeImageIconCover(imageIcon: ImageIcon, targetWidth: Int, targetHeight: Int): ImageIcon {
        val originalImage = imageIcon.image
        val originalWidth = originalImage.getWidth(null)
        val originalHeight = originalImage.getHeight(null)

        if (originalWidth <= 0 || originalHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return imageIcon // 如果原始图片或目标尺寸无效，则返回原始 ImageIcon 或考虑返回空 ImageIcon
        }

        // 计算宽度和高度的缩放比例
        val widthRatio = targetWidth.toDouble() / originalWidth.toDouble()
        val heightRatio = targetHeight.toDouble() / originalHeight.toDouble()

        // 选择较大的缩放比例，以确保图片能够 cover 目标区域
        val scaleRatio = widthRatio.coerceAtLeast(heightRatio)

        // 计算缩放后的宽度和高度
        val scaledWidth = (originalWidth * scaleRatio).toInt()
        val scaledHeight = (originalHeight * scaleRatio).toInt()

        // 使用 Image.SCALE_SMOOTH 算法进行高质量的缩放
        val scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)

        return ImageIcon(scaledImage)
    }
}