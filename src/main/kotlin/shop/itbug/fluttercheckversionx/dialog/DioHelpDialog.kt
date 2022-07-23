package shop.itbug.fluttercheckversionx.dialog

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import java.awt.Dimension
import javax.swing.*


const val helpText = """
# 安装依赖

在你的项目中添加插件依赖
```yaml
dd_check_plugin: any  #或者使用最新版
```

# 初始化

在合适的地方进行初始化,Dio()换成你的自己的dio实例
```Dart
await DdCheckPlugin.instance.init(Dio()); 
```

接入完成.
注意:第一次安装插件需要重启Idea
有问题请加Flutter自学QQ群:__667186542__

"""

/**
 * 帮助窗口
 */
class DioHelpDialog(val project: Project) : DialogWrapper(project) {


    init {
        title = "Dio Request使用教程"
        init()
        setOKButtonText("我知道了")
        setCancelButtonText("关闭")
        isResizable = false
    }


    override fun getInitialSize(): Dimension {
        return  Dimension(370,420)
    }
    override fun createCenterPanel(): JComponent? {
        val box = Box.createVerticalBox()
        val html = MarkdownShowComponent.getDocComp(helpText, project,htmlText = false)
        html.preferredSize = Dimension(this.preferredSize.width,340)
        html.removeCornerMenu()
        box.add(html)
        box.add(Box.createVerticalGlue())
        box.add(Box.createVerticalGlue())
        box.add(createButtons())
        return box
    }

    override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
        buttons.removeIf { it.actionCommand == "Cancel" }
        return super.createButtonsPanel(buttons)
    }

    private fun createButtons(): Box {
        val githubBtn = JButton("Github")
        githubBtn.addActionListener {
            BrowserUtil.open("https://github.com/mdddj/dd_check_plugin")
        }
        val pubBtn = JButton("Pub")
        pubBtn.addActionListener {
            BrowserUtil.open("https://pub.dev/packages/dd_check_plugin")
        }

        val box = Box.createHorizontalBox()
        box.add(JLabel("插件地址:"))
        box.add(githubBtn)
        box.add(pubBtn)
        return box
    }
}