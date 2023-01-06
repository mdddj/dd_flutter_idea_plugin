package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import javax.swing.SwingUtilities

/**
 * 设置面板
 */
fun settingPanel(
    model: AppStateModel,
    parentDisposable: Disposable,
    onChange: (state: AppStateModel) -> Unit
): DialogPanel {

    val alarm = Alarm(parentDisposable)
    lateinit var myPanel: DialogPanel

    myPanel = panel {


        group("典典账号登录") {
            row {
                button("登录") {
                    val project = ProjectManager.getInstance().defaultProject
                    MyNotificationUtil.showLoginDialog(project, myPanel, parentDisposable)
                }
            }
        }

        row("Diox ${PluginBundle.get("setting.listening.port")}") {
            textField().bindText(model::serverPort)
        }
        row(PluginBundle.get("setting.language")) {
            segmentedButton(listOf("System", "中文", "English")) { it }.bind(object :
                ObservableMutableProperty<String> {
                override fun set(value: String) {
                    model.lang = value
                }

                override fun afterChange(listener: (String) -> Unit) {
                }

                override fun afterChange(listener: (String) -> Unit, parentDisposable: Disposable) {
                }

                override fun get(): String {
                    return model.lang
                }

            })
        }.comment(PluginBundle.get("setting.reset.tip"))

        row(PluginBundle.get("setting.new.tips")) {
            checkBox(PluginBundle.get("open")).bindSelected(model::apiInToolwindowTop)
        }.comment(PluginBundle.get("comment.api.new.tips"))

        group("Assets 资产文件智能提醒设置") {
            row("触发文本") {
                textField().bindText(model::assetCompilationTriggerString)
            }.comment("字符串类型的输入类型中,监测到该关键字会触发智能提醒实现资产文件自动补全")
            row("触发长度") {
                intTextField().bindIntText(model::assetCompilationTriggerLen)
            }.comment("默认3,一般使用默认值就可以")
            row("扫描文件夹") {
                textField().bindText(model::assetScanFolderName)
            }.comment("默认assets文件夹")
        }
    }

    fun initValidation() {
        alarm.addRequest({
            val isModified = myPanel.isModified()
            if (isModified) {
                onChange.invoke(model)
            }
            initValidation()
        }, 1000)
    }

    val disposable = Disposer.newDisposable()
    myPanel.registerValidators(disposable)
    Disposer.register(parentDisposable, disposable)

    SwingUtilities.invokeLater {
        initValidation()
    }

    return myPanel
}