package shop.itbug.fluttercheckversionx.util

import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import com.jetbrains.lang.dart.psi.impl.DartArgumentListImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartVarInitImpl
import org.intellij.images.index.ImageInfoIndex
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.inlay.dartfile.DartStringIconShowInlay.FileResult
import shop.itbug.fluttercheckversionx.socket.formatSize
import java.io.File
import java.net.URI
import java.net.URLConnection
import kotlin.math.roundToInt

/**
 * dart psi 的相关帮助类
 */
object DartPsiElementHelper {


    private fun findDartStringLiteralInParent(element: PsiElement): DartStringLiteralExpressionImpl? {
        val secondParent = element.parent?.parent ?: return null
        return PsiTreeUtil.findChildOfType(secondParent, DartStringLiteralExpressionImpl::class.java)
    }

    /**
     * 获取最终文档的节点
     */
    fun findTargetFilePsiElement(element: PsiElement): DartStringLiteralExpressionImpl? {
        //获取目录元素
        fun PsiElement?.getTarget(): DartStringLiteralExpressionImpl? {
            if (this == null) {
                return null
            }
            if (this is LeafPsiElement && this.parent is DartStringLiteralExpression) {
                return parent as DartStringLiteralExpressionImpl
            }
            return null
        }


        //获取指向
//        fun PsiElement.getRf(): DartStringLiteralExpressionImpl? {
//            if (this is LeafPsiElement && this.parent.parent is DartReferenceExpressionImpl) {
//                val ref = this.parent.parent as DartReferenceExpressionImpl
//                val resolvePsi = ref.resolve() ?: return null
//                if (resolvePsi is DartComponentNameImpl) {
//                    val findDartStringLiteralInParent = findDartStringLiteralInParent(resolvePsi)
//                    return findDartStringLiteralInParent
//                }
//            }
//            return null
//        }

        val first = element.getTarget()
        if (first != null) {
            return first
        }

//        return element.getRf()
        return null
    }

    fun isImageFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) {
            return false
        }
        val mimeType = URLConnection.guessContentTypeFromName(file.name)
        return mimeType?.startsWith("image") == true
    }

    /**
     * 检测是否有本地文件引用
     */
    fun checkHasFile(psiElement: PsiElement): FileResult? {

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
        val maxImageSize = 300
        val fileResult = checkHasFile(element) ?: return null
        val vf = LocalFileSystem.getInstance().findFileByIoFile(fileResult.file) ?: return null
        val imageInfo = ImageInfoIndex.getInfo(vf, element.project) ?: return null
        var width = imageInfo.width
        var height = imageInfo.height
        val maxSize = width.coerceAtLeast(height)
        if (maxSize > maxImageSize) {
            val scale = maxImageSize.toDouble() / maxSize.toDouble()
            height = (height * scale).roundToInt()
            width = (width * scale).roundToInt()
        }
        val url = URI("file", null, fileResult.full, null).toString()
        val img = HtmlChunk.tag("img")
            .attr("src", url)
            .attr("width", width)
            .attr("height", height)


        val len = fileResult.file.length()
        val infos = MySimpleInfoChunk().body {
            addLink("Path", url, fileResult.basePath)
            addKeyValue(
                "Size",
                "${width}x$height"
            )
            addKeyValue(
                "Aspect ratio", calculateAspectRatio(
                    width,
                    height
                )
            )
            addKeyValue("Length", formatSize(len))
            if (PluginConfig.getState(element.project).showRewardAction) {
                addLink(PluginBundle.get("reward"), "https://itbug.shop/static/ds.68eb4cac.jpg", "❤️投喂梁典典咖啡")
            }

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

private fun calculateAspectRatio(width: Int, height: Int): String {
    val aspectRatio = width.toDouble() / height.toDouble()
    return String.format("%.2f", aspectRatio)
}