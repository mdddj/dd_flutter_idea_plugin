package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBImageIcon
import org.intellij.images.index.ImageInfoIndex
import java.awt.Dimension
import java.io.File
import java.util.*


object ImageFileUtil {
    val IMAGE_EXTENSIONS: MutableSet<String?> = mutableSetOf(
        "png", "jpg", "jpeg", "gif", "bmp", "svg", "webp", "ico", "tiff", "tif"
    )

    /**
     * 判断文件是不是图片
     */
    fun isImageFile(file: VirtualFile?): Boolean {
        if (file == null || file.isDirectory) {
            return false
        }
        val extension = file.extension
        // 检查扩展名是否在图片扩展名列表中
        return extension != null && IMAGE_EXTENSIONS.contains(extension.lowercase(Locale.getDefault()))
    }


    fun getIcon(file: VirtualFile, scaleSize: Int?): JBImageIcon? {

        return SwingUtil.fileToIcon(File(file.path), if (scaleSize != null && scaleSize <= 10) null else scaleSize)
    }

    fun getSize(file: VirtualFile, project: Project): Dimension {
        val imageInfo = runReadAction { ImageInfoIndex.getInfo(file, project) } ?: return Dimension()
        val width = imageInfo.width
        val height = imageInfo.height
        return Dimension(width, height)
    }

}