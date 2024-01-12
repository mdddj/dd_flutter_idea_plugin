package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.model.getPropertiesString
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import javax.swing.BorderFactory
import javax.swing.SwingUtilities


///freezed编辑区域
class FreezedCovertModelWidget(var model: FreezedCovertModel, val project: Project, val disposable: Disposable) :
    BorderLayoutPanel() {
    private val editView = DartEditorTextPanel(project, generateFreezedModel())

    private val editor = JBScrollPane(editView.component).apply {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
    }

    init {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
        addToCenter(editor)
        addToBottom(getSettingPanel())
    }

    /**
     * 生成freezed类
     */
    private fun generateFreezedModel(): String {
        val genFreezedClass = MyDartPsiElementUtil.genFreezedClass(
            project, model.className, model.getPropertiesString(), model.addConstructorFun, model.addFromJson
        )
        return genFreezedClass.text ?: ""
    }


    private fun changeText(value: String) {
        runWriteAction {
            editView.text = value
        }
    }

    private fun changeModel(newModel: FreezedCovertModel) {
        val genFreezedClass = MyDartPsiElementUtil.genFreezedClass(
            project, newModel.className, newModel.getPropertiesString(), model.addConstructorFun, model.addFromJson
        )
        changeText(genFreezedClass.text)
    }


    private fun getSettingPanel(): DialogPanel {
        return freezedCovertModelSetting(model, disposable) { changeModel(model) }
    }


    val code: String get() = editView.text

}


///设置面板
fun freezedCovertModelSetting(
    model: FreezedCovertModel,
    parentDispose: Disposable,
    onChanged: () -> Unit
): DialogPanel {
    lateinit var p: DialogPanel

    val alarm = Alarm(parentDispose)

    fun listenDataChanged() {
        alarm.addRequest({
            val modified = p.isModified()
            if (modified) {
                p.apply()
                onChanged()
            }
            listenDataChanged()
        }, 500)

    }

    p = panel {

        row(PluginBundle.get("rename")) {
            textField().bindText(model::className)
        }

        row(PluginBundle.get("hump.variable")) {
            checkBox(PluginBundle.get("variable.is.named.with.hump")).bindSelected(model::upperCamelStyle)
        }

        row(PluginBundle.get("default.value")) {
            checkBox(PluginBundle.get("default.value.tip")).bindSelected(model::useDefaultValueIfNull)
        }

        row(PluginBundle.get("addConstructorFun")) {
            checkBox(PluginBundle.get("addConstructorFun")).bindSelected(model::addConstructorFun)
        }

        row(PluginBundle.get("addFromJson")) {
            checkBox(PluginBundle.get("addFromJson")).bindSelected(model::addFromJson)
        }
    }


    SwingUtilities.invokeLater {
        listenDataChanged()
    }


    return p.apply {
        border = null
    }
}