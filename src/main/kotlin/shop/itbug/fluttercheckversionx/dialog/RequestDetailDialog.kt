package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import shop.itbug.fluttercheckversionx.document.Helper
import shop.itbug.fluttercheckversionx.form.socket.Request
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

///请求详情
class RequestDetailPanel(val project: Project): JPanel(BorderLayout()) {

    private val htmlView= MarkdownShowComponent.getDocComp("11",project)

    init {
        border = BorderFactory.createEmptyBorder()
        add(htmlView,BorderLayout.CENTER)
    }


    /**
     * 更新html内容
     */
    fun changeRequest(request: Request) {
        htmlView.changeMarkdown(getContentText(request))
    }


    //返回的是html类型的文本
    private fun getContentText(request: Request): String {
        val sb = StringBuilder()
        Helper.addKeyValueHeader(sb)
        //请求链接
        Helper.addKeyValueSection("Url", request.url,sb)

        //请求方法
        Helper.addKeyValueSection("Method",request.methed,sb)

        //请求耗时
        Helper.addKeyValueSection("Time","${request.timesatamp}ms",sb)

        //请求头
        Helper.addKeyValueSection("Header", getHeaderHtmlText(request.headers),sb)

        //参数
        Helper.addKeyValueSection("Param", getHeaderHtmlText(request.queryParams),sb)

        //返回的请求头
        Helper.addKeyValueSection("Response Header", getHeaderHtmlText(request.responseHeaders),sb)

        Helper.addKeyValueFoot(sb)
        return sb.toString()
    }


    private fun getHeaderHtmlText(mapParam: Map<String,Any>): String{
        if(mapParam.isEmpty()){
            return "空"
        }
        val sb = StringBuilder()
        Helper.addMarkdownTableHeader("key","Value",sb=sb)
        mapParam.forEach { (t, u) -> Helper.addMarkdownTableLine(t,"$u",sb=sb) }
//        return Helper.markdown2Html(sb.toString(),project)
        //todo
        return ""
    }

}