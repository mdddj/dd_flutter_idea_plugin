package actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys

const val PUB_URL = "https://pub.dev/packages/"

/// 选中插件唤醒浏览器打开pub对应的插件页面
class ToBrowser : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val psiElement = e.getData(PlatformDataKeys.PSI_ELEMENT)
        if (psiElement != null) {
            val text = psiElement.text
            if (text != null) {
                print(text)
                if (text.contains(": ^") || text.contains(": any")) {
                    val pluginName = text.split(":")[0];
                    BrowserUtil.browse("$PUB_URL$pluginName")
                }
            }
        }

    }
}