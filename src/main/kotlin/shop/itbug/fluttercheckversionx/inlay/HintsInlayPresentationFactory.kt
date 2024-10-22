package shop.itbug.fluttercheckversionx.inlay

import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ImageLoader
import com.intellij.util.ui.JBImageIcon
import shop.itbug.fluttercheckversionx.actions.PUB_URL
import shop.itbug.fluttercheckversionx.config.PluginSetting
import shop.itbug.fluttercheckversionx.icons.MyIcons
import java.awt.Image
import java.awt.Point
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.Icon


/**
 * 悬浮提示的工厂类
 * 提供了一些的悬浮提示工具函数
 */
typealias InlayPresentationClickHandle = (MouseEvent, Point) -> Unit


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


    fun blockIconAndText(editor: Editor, element: PsiElement, icon: Icon, text: String): InlayPresentation {
        val lWidth = lineStart(editor, element)
        val iText = iconText(icon, text)
        return factory.seq(lWidth, iText)
    }

    fun blockText(editor: Editor, element: PsiElement, text: String): InlayPresentation {
        val w = lineStart(editor, element)
        return factory.seq(w, factory.smallText(text).addRoundBg())
    }

    private fun lineStart(editor: Editor, element: PsiElement): InlayPresentation {
        val indent = editor.getIndent(element)
        val indentText = StringUtil.repeat("\t\t", indent)
        return factory.text(indentText)
    }


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

    fun lineStartText(editor: Editor, element: PsiElement, text: String): InlayPresentation {
        return factory.seq(lineStart(editor, element), factory.smallText(text).addRoundBg())
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

    fun simpleTextWithClick(text: String, tip: String?): InlayPresentation {
        return text(text).bg().addTip(tip ?: text)
    }

    fun menuActions(pluginName: String): InlayPresentation {
        return factory.mouseHandling(
            base = dartIcon().addTip("Click to operate on the plug-in package"),
            clickListener = { event, _ ->

                // 插件图标项目被点击
                JBPopupFactory.getInstance().createListPopup(pluginMenusActionPopup {
                    when (it) {
                        // 打开pub.dev对应的插件详细页面
                        actionMenus[0].key -> BrowserUtil.browse("$PUB_URL$pluginName")
                    }
                })
                    .show(RelativePoint(event.locationOnScreen))
            },
            hoverListener = object : InlayPresentationFactory.HoverListener {
                override fun onHover(event: MouseEvent, translated: Point) {
                }

                override fun onHoverFinished() {
                }

            }
        )
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


    private fun pluginMenusActionPopup(itemSelected: (key: String) -> Unit): BaseListPopupStep<String> =
        MyPluginMenusActionPopup(actionMenus, itemSelected)


    fun getImageWithPath(path: String, basePath: String, setting: PluginSetting): InlayPresentation? {
        return try {
            val file = File(path)

            // 检查文件是否存在且非空
            if (!file.exists() || file.length() == 0L) {
                return null
            }

            // 使用 ImageIcon 读取图片并缩放
            var image: Image = ImageLoader.loadCustomIcon(file) ?: return null

            // 确保图片的宽度和高度是正值
            if (image.getWidth(null) <= 0 || image.getHeight(null) <= 0) {
                return null
            }
            image = ImageLoader.scaleImage(image, setting.assetsIconSize, setting.assetsIconSize)
            val scaledIcon = JBImageIcon(image)
            // 返回图标并添加提示
            return factory.icon(scaledIcon).addTip(basePath)
        } catch (_: Exception) {
            null
        }
    }

    /// 自定义菜单弹窗
    class MyPluginMenusActionPopup(private val items: List<MenuItem>, private val itemSelected: (key: String) -> Unit) :
        BaseListPopupStep<String>() {

        init {
            val titles = items.map { it.title }
            val icons = items.map { it.icon }
            super.init(
                "Please select your action", titles, icons
            )
        }

        override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
            if (selectedValue != null) {
                val find: MenuItem? = items.find { it.title == selectedValue }
                if (find != null) {
                    itemSelected(find.key)
                }
            }
            return super.onChosen(selectedValue, finalChoice)
        }


    }

    data class MenuItem(
        val title: String,
        val icon: Icon,
        val key: String,
    )
}