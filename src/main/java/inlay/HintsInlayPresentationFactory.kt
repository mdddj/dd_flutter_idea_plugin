package inlay

import actions.PUB_URL
import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.ui.Colors
import com.intellij.ui.awt.RelativePoint
import icons.MyIcons
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon

/**
 * 悬浮提示的工厂类
 * 提供了一些的悬浮提示工具函数
 */
class HintsInlayPresentationFactory(private val factory: PresentationFactory) {



    fun simpleText(text: String,tip: String?): InlayPresentation{
        return text(text).con().bg().addTip(tip ?: text)
    }

    fun menuActions(psiElement: PsiElement, pluginName: String): InlayPresentation {
        return factory.mouseHandling(
            base = dartIcon().addTip("点击可对插件包的操作").con(),
            clickListener = object : InlayPresentationFactory.ClickListener {

                // 插件图标项目被点击
                override fun onClick(event: MouseEvent, translated: Point) {
                    JBPopupFactory.getInstance().createListPopup(pluginMenusActionPopup{
                            when (it) {

                                // 打开pub.dev对应的插件详细页面
                                actionMenus[0].key -> BrowserUtil.browse("$PUB_URL$pluginName")
                            }
                    })
                        .show(RelativePoint(event.locationOnScreen))
                }

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

    private fun InlayPresentation.con(): InlayPresentation = factory.container(
        this,
            InlayPresentationFactory.Padding(
                5,5,2,2,
            ),
        InlayPresentationFactory.RoundedCorners(

            10,10
        ),



    )


    private val actionMenus = listOf(
        MenuItem("前往pub详情页面查看",AllIcons.Toolwindows.WebToolWindow,"pub"),
        MenuItem("查看插件更新日志",AllIcons.Actions.Preview,"log"),
        MenuItem("详细信息",AllIcons.Actions.More,"info"),
    )



    fun pluginMenusActionPopup(itemSelected: (key: String) -> Unit): BaseListPopupStep<String> = MyPluginMenusAvtionPopup(actionMenus,itemSelected)


    /// 自定义菜单弹窗
    class MyPluginMenusAvtionPopup(private val items: List<MenuItem>, private val itemSelected: (key: String)-> Unit) : BaseListPopupStep<String>() {

        init {
            val titles = items.map { it.title }
            val icons = items.map { it.icon }
            super.init(
                "请选择你的操作", titles ,icons
            )
        }

        override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
            if (selectedValue!=null) {
                val find: MenuItem? = items.find { it.title == selectedValue }
                if (find!=null){
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