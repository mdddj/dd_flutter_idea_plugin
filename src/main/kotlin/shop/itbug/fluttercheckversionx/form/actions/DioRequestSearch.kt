package shop.itbug.fluttercheckversionx.form.actions

import shop.itbug.fluttercheckversionx.bus.DioWindowApiSearchBus
import shop.itbug.fluttercheckversionx.widget.MyComboActionNew
import javax.swing.event.DocumentEvent


class DioApiSearchAction : MyComboActionNew.MySearchAnAction() {

    override fun changeText(text: String, e: DocumentEvent) {
        DioWindowApiSearchBus.fire(text)
    }

}

/**
 * 接口搜索框,当用户输入接口URL后,只显示匹配的结果
 */
//class DioRequestSearch : SearchTextField(), DocumentListener {
//
//    init {
//        addDocumentListener(this)
//    }
//    /**
//     * 提取用户输入的字符串
//     */
//    override fun insertUpdate(e: DocumentEvent?) {
//        handleDocument(e)
//    }
//
//
//    private fun handleDocument(e: DocumentEvent?) {
//        e?.document?.apply { DioWindowApiSearchBus.fire(getText(0, length)) }
//    }
//
//    override fun removeUpdate(e: DocumentEvent?) {
//        handleDocument(e)
//    }
//
//    override fun changedUpdate(e: DocumentEvent?) {
//    }
//}