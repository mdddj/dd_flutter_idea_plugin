package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.dsl.addBorder
import javax.swing.Action
import javax.swing.JComponent

//生成资产文件的配置弹窗
class AssetsAutoGenerateClassActionConfigDialog(project: Project) : DialogWrapper(project) {



    init {
        super.init()
        title = "生成资产文件类"
        isResizable = false
    }

    override fun createCenterPanel(): JComponent {
        return generateAssetsFileSettingDialog {
            println(it)
            doOKAction()
        }
    }


    override fun createActions(): Array<Action> {
        return emptyArray()
    }
}

fun generateAssetsFileSettingDialog(onApply: (model: GenerateAssetsClassConfigModel) -> Unit): DialogPanel {
     val configModel: GenerateAssetsClassConfigModel = GenerateAssetsClassConfig.getGenerateAssetsSetting()
        lateinit var p : DialogPanel
     p =   panel {
        row("类    名") {
            textField().bindText(configModel::className)
        }
        row("文件名") {
            textField().bindText(configModel::fileName)
        }
        row("目    录") {
            textField().bindText(configModel::path)
        }
        row {
            checkBox("保存且不再提醒").bindSelected(configModel::dontTip)
        }.contextHelp("可在设置中再次配置")
        row {
            checkBox("监听文件变化自动更新").bindSelected(configModel::autoListenFileChange)
        }.bottomGap(BottomGap.SMALL)
        row {
            button("生成") {
                p.apply()
                GenerateAssetsClassConfig.getInstance().loadState(configModel)
                onApply.invoke(configModel)
            }.align(Align.FILL)
        }
        row {
            comment("有任何问题请<a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues'>提交bug</a>")
        }
    }
    return p.addBorder()
}