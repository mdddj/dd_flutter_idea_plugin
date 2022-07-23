package shop.itbug.fluttercheckversionx.dialog

import com.alibaba.fastjson2.JSON
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import shop.itbug.fluttercheckversionx.dialog.components.MarkdownShowComponent
import shop.itbug.fluttercheckversionx.document.Helper
import shop.itbug.fluttercheckversionx.form.socket.Request
import java.awt.Dimension
import javax.swing.Box
import javax.swing.JComponent

///请求详情查看
class RequestDetailDialog(val project: Project, private val request: Request): DialogWrapper(project) {


    init {
        init()
        title = "请求详情"
    }


    //正文内容
    override fun createCenterPanel(): JComponent {
        val coreBox = Box.createVerticalBox()
        val htmlView= MarkdownShowComponent.getDocComp(getContentText(),project)
        coreBox.add(htmlView)
        return coreBox
    }

    //窗口大小
    override fun getPreferredSize(): Dimension {
        return Dimension(500,400)
    }


    //构建markdown文本
    private fun getContentText(): String {
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
        Helper.addKeyValueSection("Body param", request.body.toString(),sb)

        //参数
        Helper.addKeyValueSection("Query param", getHeaderHtmlText(request.queryParams),sb)

        //返回的请求头
        Helper.addKeyValueSection("Response Header", getHeaderHtmlText(request.responseHeaders),sb)

        Helper.addKeyValueFoot(sb)
        println(sb.toString())
        return sb.toString()
    }


    private fun getHeaderHtmlText(mapParam: Map<String,Any>): String{
        if(mapParam.isEmpty()){
            return "空"
        }
        val sb = StringBuilder()
        Helper.addMarkdownTableHeader("key","Value",sb=sb)
        mapParam.forEach { (t, u) -> Helper.addMarkdownTableLine(t,"$u",sb=sb) }
        return Helper.markdown2Html(sb.toString(),project)
    }
    //静态方法
    companion object {

        //显示请求详情弹窗
        fun show(project: Project,request: Request) {
            RequestDetailDialog(project, request).show()
        }
    }
}