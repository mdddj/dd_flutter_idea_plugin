package shop.itbug.fluttercheckversionx.form.socket

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import javax.swing.Icon

class LeftActionTools : DefaultActionGroup() {

    init {
        add(SortAction().action)
    }

    companion object {
        fun create(): ActionToolbar {
            return ActionManager.getInstance().createActionToolbar(
                "Dio Tool Left Action",
                LeftActionTools(),
                false
            )
        }
    }
}

class SortAction : com.intellij.openapi.actionSystem.impl.ActionButton(
    object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            println("执行了切换排序操作")
        }
    },
    Presentation("使用倒序的方式渲染列表"),
    "Dio Tool Left Action Sort",
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
){

    override fun getIcon(): Icon {
        return AllIcons.ObjectBrowser.SortByType
    }
}

