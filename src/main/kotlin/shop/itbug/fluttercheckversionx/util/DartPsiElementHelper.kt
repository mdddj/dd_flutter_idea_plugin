package shop.itbug.fluttercheckversionx.util

import com.intellij.lang.documentation.DocumentationMarkup.CONTENT_END
import com.intellij.lang.documentation.DocumentationMarkup.CONTENT_START
import com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_END
import com.intellij.lang.documentation.DocumentationMarkup.DEFINITION_START
import com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_END
import com.intellij.lang.documentation.DocumentationMarkup.SECTIONS_START
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_END
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_HEADER_START
import com.intellij.lang.documentation.DocumentationMarkup.SECTION_SEPARATOR
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.impl.DartArgumentListImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartVarInitImpl
import org.intellij.images.index.ImageInfoIndex
import shop.itbug.fluttercheckversionx.inlay.dartfile.DartStringIconShowInlay.FileResult
import java.io.File
import java.net.URI
import java.net.URLConnection
import kotlin.math.roundToInt

/**
 * dart psi 的相关帮助类
 */
object DartPsiElementHelper {
    /**
     * 检测是否有本地文件引用
     */
    fun checkHasFile(psiElement: PsiElement): FileResult? {

        fun findDartStringLiteralInParent(element: PsiElement): DartStringLiteralExpressionImpl? {
            val secondParent = element.parent?.parent ?: return null
            return PsiTreeUtil.findChildOfType(secondParent, DartStringLiteralExpressionImpl::class.java)
        }

        fun isImageFile(file: File): Boolean {
            if (!file.exists() || !file.isFile) {
                return false
            }
            val mimeType = URLConnection.guessContentTypeFromName(file.name)
            return mimeType?.startsWith("image") == true
        }

        fun findFileResult(ele: DartStringLiteralExpressionImpl): FileResult? {
            val dir = ele.project.guessProjectDir() ?: return null
            val url = ele.text.replace("\'", "").replace("\"", "")
            val filePath = dir.path + File.separator + url
            val file = File(filePath)
            if (file.exists() && isImageFile(file)) {
                return FileResult(file, url, filePath)
            }
            return null
        }

        val reference = psiElement.reference
        val parent = psiElement.parent

        if (psiElement is DartStringLiteralExpressionImpl) {
            return findFileResult(psiElement)
        } else if (reference != null && reference.resolve() != null && (parent is DartArgumentListImpl || parent is DartVarInitImpl)) {
            val resolvePsi = reference.resolve()!!
            if (resolvePsi is DartComponentNameImpl) {
                val findDartStringLiteralInParent = findDartStringLiteralInParent(resolvePsi)
                if (findDartStringLiteralInParent != null) {
                    return findFileResult(findDartStringLiteralInParent)
                }
            }
        }
        return null
    }

    /**
     * 生成本机图片类型的文档html
     */
    fun generateLocalImageDocument(element: PsiElement): String? {
        val MAX_IMAGE_SIZE = 300
        val fileResult = checkHasFile(element) ?: return null
        val vf = LocalFileSystem.getInstance().findFileByIoFile(fileResult.file) ?: return null
        val imageInfo = ImageInfoIndex.getInfo(vf, element.project) ?: return null
        var width = imageInfo.width
        var height = imageInfo.height
        val maxSize = width.coerceAtLeast(height)
        if (maxSize > MAX_IMAGE_SIZE) {
            val scale = MAX_IMAGE_SIZE.toDouble() / maxSize.toDouble()
            height = (height * scale).roundToInt()
            width = (width * scale).roundToInt()
        }
        val url = URI("file", null, fileResult.full, null).toString()
        val img = HtmlChunk.tag("img")
            .attr("src", url)
            .attr("width", width)
            .attr("height", height)

        val infos = MySimpleInfoChunk().body {
            addKeyValue("Path", fileResult.full)
            addKeyValue("Width", imageInfo.width.toString() + "px")
            addKeyValue("Height", imageInfo.height.toString() + "px")
            addKeyValue("Bpp", imageInfo.bpp.toString())
            addKeyValue("Size", "${fileResult.file.length()}")
            addLink("打赏", "https://itbug.shop/static/ds.68eb4cac.jpg", "请梁典典喝杯咖啡")
        }

        val html = HtmlBuilder()
            .append(img).br().append(HtmlChunk.div().addRaw(infos.toString()))
            .toString()
        return html
    }

}

class MySimpleInfoChunk(title: String? = null, value: String? = null) {
    private val sb = StringBuilder()

    init {
        if (title != null) {
            sb.append(DEFINITION_START)
            sb.append(title)
            sb.append(DEFINITION_END)
        }
        if (value != null) {
            sb.append(CONTENT_START)
            sb.append(value)
            sb.append(CONTENT_END)
        }


    }

    fun bodyHeader() {
        sb.append(SECTIONS_START)
    }

    fun bodyFoot() {
        sb.append(SECTIONS_END)
    }

    fun addKeyValue(key: String, value: String) {
        sb.append(SECTION_HEADER_START)
        sb.append(key)
        sb.append(SECTION_SEPARATOR)
        sb.append(value)
        sb.append(SECTION_END)
    }

    fun addLink(title: String, src: String, text: String) {
        sb.append(SECTION_HEADER_START)
        sb.append(title)
        sb.append(SECTION_SEPARATOR)
        sb.append(
            HtmlChunk.tag("a").attr("href", src).addText(text).toString()
        )
        sb.append(SECTION_END)

    }

    override fun toString(): String {
        return sb.toString()
    }
}

inline fun MySimpleInfoChunk.body(block: MySimpleInfoChunk.() -> Unit): MySimpleInfoChunk {
    bodyHeader()
    block()
    bodyFoot()
    return this
}
