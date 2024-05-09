package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.ui.SearchTextField
import com.intellij.util.ui.JBUI
import shop.itbug.fluttercheckversionx.bus.DioWindowApiSearchBus
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.widget.MyComboActionNew
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class DioApiSearchAction : MyComboActionNew.MySearchAnAction() {

    override fun changeText(text: String, e: DocumentEvent) {
        DioWindowApiSearchBus.fire(text)
    }

}

/**
 * 接口搜索框,当用户输入接口URL后,只显示匹配的结果
 */
class DioRequestSearch : SearchTextField(), DocumentListener {

    init {
        addDocumentListener(this)
        textEditor.border = JBUI.Borders.empty()
        textEditor.emptyText.text = PluginBundle.get("dio.search.empty.text")
    }

    /**
     * 提取用户输入的字符串
     */
    override fun insertUpdate(e: DocumentEvent?) {
        handleDocument(e)
    }

    private fun handleDocument(e: DocumentEvent?) {
        e?.document?.apply { DioWindowApiSearchBus.fire(getText(0, length)) }
    }

    override fun removeUpdate(e: DocumentEvent?) {
        handleDocument(e)
    }

    override fun changedUpdate(e: DocumentEvent?) {
        handleDocument(e)
    }
}
