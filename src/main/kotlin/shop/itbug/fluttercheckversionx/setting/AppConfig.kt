package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBTextField
import shop.itbug.fluttercheckversionx.dsl.settingPanel
import javax.swing.JComponent

class AppConfig : Configurable {

    private val portTextFiled = JBTextField()


    override fun createComponent(): JComponent {
        return panel
    }

    private val panel: JComponent  get() = settingPanel()

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