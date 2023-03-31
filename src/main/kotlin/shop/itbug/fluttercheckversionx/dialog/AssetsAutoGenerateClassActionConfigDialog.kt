package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import javax.swing.Action
import javax.swing.JComponent

//生成资产文件的配置弹窗
class AssetsAutoGenerateClassActionConfigDialog(project: Project) : DialogWrapper(project) {

    private val configModel: GenerateAssetsClassConfigModel = GenerateAssetsClassConfig.getInstance().state

    init {
        super.init()
        title = "生成资产文件类"
        isResizable = false
    }

    override fun createCenterPanel(): JComponent {
        return ui
    }

    val ui: DialogPanel
        get() : DialogPanel {
            val p : DialogPanel = panel {
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
                        ui.apply()
                        println(configModel)
                        GenerateAssetsClassConfig.getInstance().loadState(configModel)
                        super.doOKAction()
                    }.align(Align.FILL)
                }
            }
            return p
        }

    override fun createActions(): Array<Action> {
        return emptyArray()
    }
}

