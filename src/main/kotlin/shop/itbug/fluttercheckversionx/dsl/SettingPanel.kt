package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.services.AppStateModel

/**
 * 设置面板
 */
fun settingPanel (model : AppStateModel) : DialogPanel {

    return panel {
        row("dio 监听端口") {
            textField().bindText(model::serverPort)
            button("保存") {

            }
        }
        row ("Language") {
            segmentedButton(listOf("System","中文","English")) { it }.bind(object : ObservableMutableProperty<String> {
                override fun set(value: String) {
                    println("设置语言:$value")
                    model.lang = value
                }
                override fun afterChange(listener: (String) -> Unit) {
                }

                override fun afterChange(listener: (String) -> Unit, parentDisposable: Disposable) {
                }

                override fun get(): String {
                    println("初始化语言:${model.lang}")
                   return model.lang
                }

            })
            label("更改后需要重启生效")
        }
    }
}