package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import javax.swing.SwingUtilities

/**
 * 设置面板
 */
fun settingPanel(
    model: AppStateModel,
    dioxSetting: DoxListeningSetting,
    parentDisposable: Disposable,
    onChange: (state: AppStateModel) -> Unit
): DialogPanel {

    val alarm = Alarm(parentDisposable)
    val languageList = listOf("System", "中文", "繁體", "English", "한국어", "日本語")

    val myPanel: DialogPanel = panel {

        row {
            button(PluginBundle.get("window.chat.loginDialog.text")) {
                val project = ProjectManager.getInstance().defaultProject
                MyNotificationUtil.showLoginDialog(project)
            }
        }

//
        buttonsGroup(PluginBundle.get("basic")) {
            row(PluginBundle.get("setting.language")) {
                for (lan in languageList)
                    radioButton(lan, value = lan)
            }.comment(PluginBundle.get("setting.reset.tip"))
        }.bind({ model.lang }, { model.lang = it })

//        group(PluginBundle.get("basic")) {
//
//
//            row(PluginBundle.get("setting.language")) {
//
//
//                segmentedButton(listOf("System", "中文", "繁體", "English", "한국어", "日本語")) { it }.bind(object :
//                    ObservableMutableProperty<String> {
//                    override fun set(value: String) {
//                        model.lang = value
//                    }
//
//                    override fun afterChange(listener: (String) -> Unit) {
//                    }
//
//                    override fun afterChange(listener: (String) -> Unit, parentDisposable: Disposable) {
//                    }
//
//                    override fun get(): String {
//                        return model.lang
//                    }
//
//                })
//            }
//                .comment(PluginBundle.get("setting.reset.tip"))
//
//        }

        group("Dio") {
            row("Dio ${PluginBundle.get("setting.listening.port")}") {
                textField().bindText(model::serverPort)
            }.comment(PluginBundle.get("setting.reset.tip"))


//            新接口弹出提醒
            row(PluginBundle.get("setting.new.tips")) {
                checkBox(PluginBundle.get("open")).bindSelected(model::apiInToolwindowTop)
            }
            //.comment(PluginBundle.get("comment.api.new.tips"))


            indent {
                twoColumnsRow({
                    checkBox(PluginBundle.get("display_domain_name")).bindSelected(dioxSetting::showHost)
                }, {
                    checkBox(PluginBundle.get("display.query.parameters")).bindSelected(dioxSetting::showQueryParams)
                })
                twoColumnsRow({
                    checkBox(PluginBundle.get("show.request.method")).bindSelected(dioxSetting::showMethod)
                }, {
                    checkBox(PluginBundle.get("display.status.code")).bindSelected(dioxSetting::showStatusCode)
                })
                twoColumnsRow({
                    checkBox(PluginBundle.get("display.time")).bindSelected(dioxSetting::showTimestamp)
                }, {
                    checkBox(PluginBundle.get("time")).bindSelected(dioxSetting::showDate)
                })
                twoColumnsRow({
                    checkBox(PluginBundle.get("bold.link")).bindSelected(dioxSetting::urlBold)
                }, {})
            }

        }

        group(PluginBundle.get("ass.setting.title")) {
            row(PluginBundle.get("ass.1")) {
                textField().bindText(model::assetCompilationTriggerString)
            }
            row(PluginBundle.get("ass.3")) {
                intTextField().bindIntText(model::assetCompilationTriggerLen)
            }
            row(PluginBundle.get("ass.5")) {
                textField().bindText(model::assetScanFolderName)
            }
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