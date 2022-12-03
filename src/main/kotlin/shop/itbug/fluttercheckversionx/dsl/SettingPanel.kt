package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

/**
 * 设置面板
 */
fun settingPanel () : DialogPanel {
    return panel {
        row("dio 监听端口") {
            textField()
        }
        row ("Language") {
            segmentedButton(listOf("System","中文","English")) { it }.whenItemSelected {
                println("change it:$it")
            }
        }
    }
}