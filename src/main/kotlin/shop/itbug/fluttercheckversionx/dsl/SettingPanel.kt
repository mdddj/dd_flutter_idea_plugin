package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.Alarm
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.constance.Links
import shop.itbug.fluttercheckversionx.constance.documentCommentRow
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
import shop.itbug.fluttercheckversionx.setting.PubDevMirrorImageSetting
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
    lateinit var urlInput: Cell<JBTextField>
    val myPanel: DialogPanel = panel {
        buttonsGroup(PluginBundle.get("basic")) {
            row(PluginBundle.get("setting.language")) {
                for (lan in languageList)
                    radioButton(lan, value = lan)
            }.comment(PluginBundle.get("setting.reset.tip"))
        }.bind({ model.lang }, { model.lang = it })

        group(PluginBundle.get("check_flutter_version_title")) {

            row {
                checkBox(PluginBundle.get("open")).bindSelected(dioxSetting::checkFlutterVersion)
            }.comment(PluginBundle.get("check_flutter_version_comment"))
            row("Url") {
                urlInput = textField().bindText(dioxSetting::checkFlutterVersionUrl).align(Align.FILL)
            }
            row {
                button(PluginBundle.get("use_default_value")) {
                    urlInput.component.text = Links.defaultFlutterInfosUrl
                }
                button("设置中国区镜像地址") {
                    urlInput.component.text = Links.defaultFlutterInfosUrlByCN
                }
            }.comment(Links.generateDocCommit(urlInput.component.text, "View"))
//            row("pub.dev host") {
//                textField().bindText(dioxSetting::pubServerUrl).align(Align.FILL)
//            }
            PubDevMirrorImageSetting.createPanel(this, dioxSetting)
            documentCommentRow(Links.checkFlutterVersion)
        }


        group("Dio") {
            row("Dio ${PluginBundle.get("setting.listening.port")}") {
                textField().bindText(model::serverPort)
            }.comment(PluginBundle.get("setting.reset.tip"))


//            新接口弹出提醒
            row(PluginBundle.get("setting.new.tips")) {
                checkBox(PluginBundle.get("open")).bindSelected(model::apiInToolwindowTop)
            }

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
                row {
                    checkBox(PluginBundle.get("dio.setting.show.data.size")).bindSelected(dioxSetting::showDataSize)
                }
            }

            row {
                comment(Links.generateDocCommit(Links.dio))
            }
        }

        group(PluginBundle.get("ass.setting.title")) {
            row("Enable") {
                checkBox("Enable").bindSelected(model::assetsScanEnable)
            }
            row(PluginBundle.get("ass.1")) {
                textField().bindText(model::assetCompilationTriggerString)
            }
            row(PluginBundle.get("ass.3")) {
                intTextField().bindIntText(model::assetCompilationTriggerLen)
            }
            row(PluginBundle.get("ass.5")) {
                textField().bindText(model::assetScanFolderName)
            }
            row {
                comment(Links.generateDocCommit(Links.accetsIcons))
            }
        }

        ///配置拷贝接口的 key
        group("Copy All Keys") {
            row("url") {
                textField().bindText(dioxSetting.copyKeys::url)
            }
            row("method") {
                textField().bindText(dioxSetting.copyKeys::method)
            }
            row("headers") {
                textField().bindText(dioxSetting.copyKeys::headers)
            }
            row("queryParams") {
                textField().bindText(dioxSetting.copyKeys::queryParams)
            }
            row("body") {
                textField().bindText(dioxSetting.copyKeys::body)
            }
            row("responseStatusCode") {
                textField().bindText(dioxSetting.copyKeys::responseStatusCode)
            }
            row("response") {
                textField().bindText(dioxSetting.copyKeys::response)
            }
            row("requestTime") {
                textField().bindText(dioxSetting.copyKeys::requestTime)
            }
            row("timestamp") {
                textField().bindText(dioxSetting.copyKeys::timestamp)
            }
            row {
                comment(Links.generateDocCommit(Links.dioImage))
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