package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.model.getPropertiesString
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.awt.BorderLayout

class FreezedCovertModelWidget(val model: FreezedCovertModel,val project: Project) : JBPanel<FreezedCovertModelWidget>(BorderLayout()) {

    private val editView = LanguageTextField(
        DartLanguage.INSTANCE,
        project,
        "",
        false
    )

    init {
        add(JBScrollPane(editView),BorderLayout.CENTER)
        generateFreezedModel()
    }

    /**
     * 生成freezed类
     */
    private fun generateFreezedModel() {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, model.className, model.getPropertiesString())
        editView.text = genFreezedClass.text
    }


    fun changeModel(newModel: FreezedCovertModel){
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, newModel.className, newModel.getPropertiesString())
        editView.text = genFreezedClass.text
    }


}