package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBScrollPane
import javax.swing.JComponent

class AppConfig : Configurable {

    override fun createComponent(): JComponent {
        return MySettingPanel()
    }

    override fun isModified(): Boolean {
        print("修改了>>>")
        return true
    }

    override fun apply() {
        print("执行了>>>>")
    }

    override fun getDisplayName(): String {
        return "Flutter CheckX Setting"
    }
}