package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.AppStateModel
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

    val p: DialogPanel = panel {


        group("典典账号登录") {
            row {
                button("登录"){}
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
            checkBox( PluginBundle.get("open")).bindSelected(model::apiInToolwindowTop)
        }.comment(PluginBundle.get("comment.api.new.tips"))

        indent {

        }
    }

    fun initValidation() {
        alarm.addRequest({
            val isModified = p.isModified()
            if (isModified) {
                onChange.invoke(model)
            }
            initValidation()
        }, 1000)
    }

    val disposable = Disposer.newDisposable()
    p.registerValidators(disposable)
    Disposer.register(parentDisposable, disposable)

    SwingUtilities.invokeLater {
        initValidation()
    }

    return p
}