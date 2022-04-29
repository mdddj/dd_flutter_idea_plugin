package shop.itbug.fluttercheckversionx.form

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.ui.AnActionButton


/// dio操作工具栏
class DioToolbar : ActionToolbar ("dio request toolbar",DefaultActionGroup.EMPTY_GROUP,true){


    init {

        add(object : AnActionButton("清空","清空请求列表",AllIcons.Ide.ConfigFile){
            override fun actionPerformed(e: AnActionEvent) {
                println("我被按下了")
            }

        }.contextComponent)

    }

}