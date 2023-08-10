package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.model.getPropertiesString
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.awt.BorderLayout

class FreezedCovertModelWidget(var model: FreezedCovertModel, val project: Project) :
    JBPanel<FreezedCovertModelWidget>(BorderLayout()) {

//    private val editView = SwingUtil.getDartEditor(project,"")
        private val editView = DartEditorTextPanel(project)

    init {
        add(JBScrollPane(editView.component), BorderLayout.CENTER)
        generateFreezedModel()
        add(getSettingPanel(), BorderLayout.SOUTH)

    }

    /**
     * 生成freezed类
     */
    private fun generateFreezedModel() {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, model.className, model.getPropertiesString(),model.addConstructorFun,model.addFromJson)
        changeText(genFreezedClass.text)
    }


    private fun changeText(value:String){
        runWriteAction {
            editView.text = value
        }
    }

    private fun changeModel(newModel: FreezedCovertModel) {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, newModel.className, newModel.getPropertiesString(),model.addConstructorFun,model.addFromJson)
        changeText(genFreezedClass.text)
    }


    private fun getSettingPanel(): DialogPanel {
        return freezedCovertModelSetting(model) { changeModel(model) }
    }


    val code: String get() = editView.text

}

fun freezedCovertModelSetting(
    model: FreezedCovertModel,
    submit: () -> Unit
): DialogPanel {
    lateinit var p: DialogPanel
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

        row (PluginBundle.get("addConstructorFun")) {
            checkBox(PluginBundle.get("addConstructorFun")).bindSelected(model::addConstructorFun)
        }

        row (PluginBundle.get("addFromJson")){
            checkBox(PluginBundle.get("addFromJson")).bindSelected(model::addFromJson)
        }

        row {
            button(PluginBundle.get("save.and.refresh")) {
                p.apply()
                submit.invoke()
            }
        }
    }
    return p
}