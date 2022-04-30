package shop.itbug.fluttercheckversionx.form.actions

import com.intellij.openapi.components.service
import com.intellij.ui.SearchTextField
import shop.itbug.fluttercheckversionx.form.socket.Request
import shop.itbug.fluttercheckversionx.socket.service.AppService
import javax.swing.BorderFactory
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

typealias FilterUrls = (result: List<Request>) -> Unit

/**
 * 接口搜索框,当用户输入接口URL后,只显示匹配的结果
 */
class DioRequestSearch (private val filterUrlHandler: FilterUrls) : SearchTextField(), DocumentListener {

    init {
        border = BorderFactory.createEmptyBorder()
        addDocumentListener(this)
    }


    private val appService get() = service<AppService>()

    /**
     * 提取用户输入的字符串
     */
    override fun insertUpdate(e: DocumentEvent?) {
        if(e!=null){
            val document = e.document
            val text = document.getText(0, document.length)
            val allRequest = appService.getAllRequest()
            val results = allRequest.filter { it.url.uppercase().contains(text.uppercase()) }
            if(results.isNotEmpty()){
                filterUrlHandler.invoke(results)
            }
        }
    }
    override fun removeUpdate(e: DocumentEvent?) {
    }
    override fun changedUpdate(e: DocumentEvent?) {
    }
}