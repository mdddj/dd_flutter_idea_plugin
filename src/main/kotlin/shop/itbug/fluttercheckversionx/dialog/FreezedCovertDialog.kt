package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.LanguageTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.model.FreezedCovertModel
import shop.itbug.fluttercheckversionx.model.getPropertiesString
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.awt.Dimension
import javax.swing.JComponent

class FreezedCovertDialog(val project: Project, val model: FreezedCovertModel) : DialogWrapper(project) {

    data class GenProperties(
        var genFromJson: Boolean = false,
        var genPartOf: Boolean = false,
        var funsToExt: Boolean = false,
         var className: String = ""
    )

    val setting = GenProperties().apply {
        className = model.className
    }

    private val editView = LanguageTextField(
        DartLanguage.INSTANCE,
        project,
        "",
        false
    ).apply {
        maximumSize = Dimension(500, 200)
        preferredSize = Dimension(500, 200)
        minimumSize = Dimension(500, 200)
    }

    init {
        super.init()
        title = "模型转Freezed对象(${model.className})"
        setSize(500, 380)
        setOKButtonText("生成")
        generateFreezedModel()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("重命名") {
                textField().bindText(setting::className)
            }
            row {
                scrollCell(editView)
                    .align(Align.FILL)
            }
            row {
                checkBox("生成fromJson方法").bindSelected({setting.genFromJson}){
                    if(it){

                    }
                }
            }
            row {
                checkBox("生成part引用").bindSelected(setting::genPartOf)
            }
            row {
                checkBox("类方法转换为扩展函数").bindSelected(setting::funsToExt)
            }
            row {
                button("预览") {

                }
            }
        }
    }


    /**
     * 生成freezed类
     */
    private fun generateFreezedModel() {
        val genFreezedClass =
            MyDartPsiElementUtil.genFreezedClass(project, model.className, model.getPropertiesString())

        editView.text = genFreezedClass.text
    }




}