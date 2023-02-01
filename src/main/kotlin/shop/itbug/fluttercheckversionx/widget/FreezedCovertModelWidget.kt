package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.model.getPropertiesString
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.awt.BorderLayout

class FreezedCovertModelWidget(var model: FreezedCovertModel, val project: Project) :
    JBPanel<FreezedCovertModelWidget>(BorderLayout()) {

    private val editView = LanguageTextField(
        DartLanguage.INSTANCE,
        project,
        "",
        false
    )

    init {
        add(JBScrollPane(editView), BorderLayout.CENTER)
        generateFreezedModel()
        add(getSettingPanel(), BorderLayout.SOUTH)
    }

    /**
     * 生成freezed类
     */
    private fun generateFreezedModel() {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, model.className, model.getPropertiesString())
        editView.text = genFreezedClass.text
    }


    private fun changeModel(newModel: FreezedCovertModel) {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, newModel.className, newModel.getPropertiesString())
        editView.text = genFreezedClass.text
    }


    private fun getSettingPanel(): DialogPanel {
        return freezedCovertModelSetting(model){
            changeModel(model)
        }
    }




}

fun freezedCovertModelSetting(model: FreezedCovertModel,submit: () -> Unit): DialogPanel {
    lateinit var p: DialogPanel
    p = panel {

        row("重命名") {
            textField().bindText(model::className)
        }

        row("驼峰变量") {
            checkBox("变量使用驼峰命名").bindSelected(model::upperCamelStyle)
        }

        row("默认值") {
            checkBox("如果变量为空设置一个默认值").bindSelected(model::useDefaultValueIfNull)
        }

        row {
            button("保存") {
                p.apply()
                submit.invoke()
            }
        }
    }
    return p
}