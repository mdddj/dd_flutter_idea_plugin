package shop.itbug.fluttercheckversionx.window.hive

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.i18n.i18n

class HiveWindow: BorderLayoutPanel() {

    init {
        addToCenter(JBLabel("future".i18n()))
    }
}