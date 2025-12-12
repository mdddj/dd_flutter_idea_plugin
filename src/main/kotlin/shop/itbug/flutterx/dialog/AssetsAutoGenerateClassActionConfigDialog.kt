package shop.itbug.flutterx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import shop.itbug.flutterx.config.GenerateAssetsClassConfig
import shop.itbug.flutterx.config.GenerateAssetsClassConfigModel
import shop.itbug.flutterx.constance.Links
import shop.itbug.flutterx.i18n.PluginBundle
import javax.swing.Action
import javax.swing.JComponent

//生成资产文件的配置弹窗
class AssetsAutoGenerateClassActionConfigDialog(val project: Project) : DialogWrapper(project) {

    private val configModel: GenerateAssetsClassConfigModel = GenerateAssetsClassConfig.getInstance(project).state

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
            val p: DialogPanel = panel {
                row(PluginBundle.get("g.1")) {
                    textField().bindText({ configModel.className ?: "" }, { configModel.className = it })
                }
                row(PluginBundle.get("g.2")) {
                    textField().bindText({ configModel.fileName ?: "" }, { configModel.fileName = it })
                }
                row(PluginBundle.get("g.3")) {
                    MyRowBuild.folder(this, { configModel.path ?: "" }, { configModel.path = it }, project)
                }
                row {
                    checkBox(PluginBundle.get("dot_show_again_this") + " this dialog").bindSelected(configModel::dontTip)
                }
                row {
                    checkBox(PluginBundle.get("g.9.1")).bindSelected(configModel::autoListenFileChange)
                }.bottomGap(BottomGap.SMALL)

                row {
                    button(PluginBundle.get("assets.gen")) {
                        ui.apply()
                        GenerateAssetsClassConfig.getInstance(project).loadState(configModel)
                        super.doOKAction()
                    }.align(Align.FILL)
                }
                row {
                    comment(Links.generateDocCommit(Links.ASSETS)).align(AlignX.RIGHT)
                }
            }
            return p
        }

    override fun createActions(): Array<Action> {
        return emptyArray()
    }
}

