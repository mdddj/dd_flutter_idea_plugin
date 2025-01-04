package shop.itbug.fluttercheckversionx.setting

import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import shop.itbug.fluttercheckversionx.config.DoxListeningSetting
import shop.itbug.fluttercheckversionx.constance.DartPubMirrorImage
import shop.itbug.fluttercheckversionx.constance.FlutterCheckUrlMirrorImage


object PubDevMirrorImageSetting {


    /// pub.dev镜像地址
    fun createPanel(panel: Panel, setting: DoxListeningSetting) {
        val defaultSelect = DartPubMirrorImage.DefaultPub
        val model = CollectionComboBoxModel(DartPubMirrorImage.entries, defaultSelect)
        panel.row("pub.dev mirror image ") {
            comboBox(model).bindItem({
                DartPubMirrorImage.entries.find { it.url == setting.pubServerUrl } ?: defaultSelect
            }) {
                setting.pubServerUrl = it?.url ?: defaultSelect.url
            }
        }.comment(createTestLink(setting.pubServerUrl))
    }

    private fun createTestLink(url: String): String {
        return HtmlChunk.link(url, "Visit  url link").toString()
    }


    /// flutter.dev镜像设置
    fun createFlutterCheckUrlPanel(panel: Panel, setting: DoxListeningSetting) {
        val defaultSelect = FlutterCheckUrlMirrorImage.DefaultUrl
        val model = CollectionComboBoxModel(FlutterCheckUrlMirrorImage.entries, defaultSelect)
        panel.row("flutter.dev mirror image") {
            comboBox(model).bindItem({
                FlutterCheckUrlMirrorImage.entries.find { it.url == setting.checkFlutterVersionUrl } ?: defaultSelect
            }) {
                setting.checkFlutterVersionUrl = it?.url ?: defaultSelect.url
            }
        }.comment(createTestLink(setting.checkFlutterVersionUrl))
    }
}

