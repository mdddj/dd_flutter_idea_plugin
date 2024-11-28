package shop.itbug.fluttercheckversionx.setting

import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.listCellRenderer.listCellRenderer
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.constance.DartPubMirrorImage
import javax.swing.ListCellRenderer


object PubDevMirrorImageSetting {
    fun createPanel(panel: Panel, setting: DoxListeningSetting) {

        val defaultSelect = DartPubMirrorImage.DefaultPub
        val model = CollectionComboBoxModel(DartPubMirrorImage.entries, defaultSelect)
        panel.row("pub.dev Mirror") {
            comboBox(model).bindItem({
                DartPubMirrorImage.entries.find { it.url == setting.pubServerUrl } ?: defaultSelect
            }) {
                setting.pubServerUrl = it?.url ?: defaultSelect.url
            }.component.apply {
                renderer = createRenderer()
            }
        }
    }
}


private fun createRenderer(): ListCellRenderer<DartPubMirrorImage> {
    return listCellRenderer {
        text(this.value.title)
    }
}
