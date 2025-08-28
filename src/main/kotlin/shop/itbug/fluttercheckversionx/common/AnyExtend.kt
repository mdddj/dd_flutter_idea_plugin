package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import shop.itbug.fluttercheckversionx.dialog.freezed.StringToFreezedDialog
import javax.swing.BorderFactory
import javax.swing.JComponent


/**
 * 设置为滚动面板
 */
fun JComponent.scroll(): JBScrollPane {
    return JBScrollPane(this).apply {
        border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
    }
}

///json转 freezed 通用函数
fun Project.jsonToFreezedRun(jsonText: String) {
    StringToFreezedDialog(this, jsonText).show()
}


