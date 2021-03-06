package shop.itbug.fluttercheckversionx.dialog

import cn.hutool.http.HttpUtil
import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import shop.itbug.fluttercheckversionx.dialog.components.changeMarkdownText
import shop.itbug.fluttercheckversionx.model.TextModelResult
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 打赏的弹窗
 */
class RewardDialog(var project: Project) : DialogWrapper(project) {
    private var view = MarkdownShowComponent( "加载中...",project)

    init {
        init()
        title = "打赏"
        setOKButtonText("谢谢支持")
        getContentText()
    }

    private fun getContentText() {
        try {
            val text = HttpUtil.get("https://itbug.shop/api/blog/text?password=&name=blog-ds")
            val model = Gson().fromJson(text, TextModelResult::class.java)
            view.changeMarkdown(model.data?.context?:"获取打赏内容失败:${model.message}")
        } catch (e: Exception) {
            view.changeMarkdown("获取打赏内容失败${e.localizedMessage}")
        }

    }

    override fun createCenterPanel(): JComponent {
        val box = Box.createVerticalBox()
        box.add(view)
        return box
    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        buttons.removeIf { it.actionCommand =="Cancel" }
        return super.createButtonsPanel(buttons)
    }

}