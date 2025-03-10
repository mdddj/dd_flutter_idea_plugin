package shop.itbug.fluttercheckversionx.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.images.fileTypes.impl.ImageFileType
import org.intellij.images.index.ImageInfoIndex
import java.awt.Dimension
import javax.swing.Icon
import kotlin.io.path.Path


object ImageFileUtil {

    /**
     * 判断文件是不是图片
     */
    fun isImageFile(file: VirtualFile?): Boolean {
        if (file == null) return false
        return ImageFileType.INSTANCE.equals(file.fileType)
    }


    fun getIcon(file: VirtualFile): Icon {
        return IconLoader.findUserIconByPath(Path(file.path))
    }

    fun getSize(file: VirtualFile, project: Project): Dimension {
        val imageInfo = ImageInfoIndex.getInfo(file, project) ?: return Dimension()
        val width = imageInfo.width
        val height = imageInfo.height
        return Dimension(width, height)
    }
}