package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.SwingUtil
import java.awt.Point
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.Icon


/**
 * 悬浮提示的工厂类
 * 提供了一些的悬浮提示工具函数
 */
typealias InlayPresentationClickHandle = (MouseEvent, Point) -> Unit


fun Editor.getLine(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    return line
}


fun Editor.getLineStart(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    return document.getLineStartOffset(line)
}

/**
 * 获取缩进长度
 * 或者可以参考@[EditorUtil#getPlainSpaceWidth]
 */
fun Editor.getIndent(element: PsiElement): Int {
    val offset = element.textRange.startOffset
    val line = document.getLineNumber(offset)
    val lineStart = document.getLineStartOffset(line)
    return offset - lineStart
}


class HintsInlayPresentationFactory(private val factory: PresentationFactory) {


    fun iconRoundClick(icon: Icon, clicked: InlayPresentationClickHandle? = null): InlayPresentation {
        return factory.smallScaledIcon(icon).addRoundBg().clickHandle(clicked)
    }

    /**
     * 图标+文字
     * @param iconIsInLeft 图标是否在左边,false: 图标在右边
     */
    fun iconText(
        icon: Icon,
        text: String,
        iconIsInLeft: Boolean = true,
        handle: InlayPresentationClickHandle? = null
    ): InlayPresentation {
        val iconInlay = factory.smallScaledIcon(icon)
        return if (iconIsInLeft) {
            factory.seq(iconInlay, factory.smallText(text)).addRoundBg().clickHandle(handle)
        } else {
            factory.seq(factory.smallText(text), iconInlay).addRoundBg().clickHandle(handle)
        }
    }

    private fun InlayPresentation.addRoundBg(): InlayPresentation {
        return factory.roundWithBackgroundAndSmallInset(this)
    }

    private fun InlayPresentation.clickHandle(handle: InlayPresentationClickHandle?): InlayPresentation {
        return factory.mouseHandling(this, clickListener = handle, null)
    }

    fun simpleText(text: String, tip: String?, handle: InlayPresentationClickHandle?): InlayPresentation {
        return text(text).bg().addTip(tip ?: text).clickHandle(handle)
    }

    private fun dartIcon(): InlayPresentation = factory.smallScaledIcon(MyIcons.dartPluginIcon)
        .insert(factory.smallScaledIcon(AllIcons.Actions.FindAndShowNextMatchesSmall)).bg()

    // 展示一个文本
    private fun text(text: String?): InlayPresentation = factory.smallText(text ?: "?")

    // 添加提示文本
    private fun InlayPresentation.addTip(text: String): InlayPresentation = factory.withTooltip(text, this)

    // 添加一个背景颜色
    private fun InlayPresentation.bg(): InlayPresentation = factory.roundWithBackgroundAndSmallInset(this)

    private fun InlayPresentation.insert(newInlay: InlayPresentation): InlayPresentation =
        factory.join(listOf(this, newInlay)) { text("") }


    private val actionMenus = listOf(
        MenuItem("Go to the pub.dev page", AllIcons.Toolwindows.WebToolWindow, "pub"),
    )


    fun getImageWithPath(path: String, basePath: String, setting: PluginSetting): InlayPresentation? {
        return try {
            val file = File(path)
            // 检查文件是否存在且非空
            if (!file.exists() || file.length() == 0L) {
                return null
            }
            val scaledIcon = SwingUtil.fileToIcon(file, setting.assetsIconSize) ?: return null
            // 返回图标并添加提示
            return factory.icon(scaledIcon).addTip(basePath)
        } catch (_: Exception) {
            null
        }
    }

    data class MenuItem(
        val title: String,
        val icon: Icon,
        val key: String,
    )
}