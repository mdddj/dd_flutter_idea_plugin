package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.project.Project

///渲染文档帮助类
class Helper {

    companion object {


        // key - value 格式
        fun addKeyValueSection(key: String, value: String, sb: java.lang.StringBuilder) {
            sb.append(DocumentationMarkup.SECTION_HEADER_START)
            sb.append(key)
            sb.append(DocumentationMarkup.SECTION_SEPARATOR)
            sb.append("<p>")
            sb.append(value)
            sb.append(DocumentationMarkup.SECTION_END)
        }

        //头
        fun addKeyValueHeader(sb: java.lang.StringBuilder){
            sb.append(DocumentationMarkup.SECTIONS_START)
        }

        //尾
        fun addKeyValueFoot(sb: java.lang.StringBuilder){
            sb.append(DocumentationMarkup.SECTIONS_END)
        }

        // markdown表格类型
        fun addTablevalue(key: String, value: String, sb: java.lang.StringBuilder) {
            sb.append("| ")
            sb.append(key)
            sb.append("| ")
            sb.split(value)
            sb.append(" |")
            sb.append("\n")
        }

        // markdown转成html
        fun markdown2Html(markdownText: String, project: Project): String {
           return MarkdownRender.renderText(markdownText, project)
        }



        fun addMarkdownTableHeader(vararg headers: String,sb: java.lang.StringBuilder){
            var headerStr = "|"
            headers.forEach {
                headerStr += " $it |"
            }
            sb.append(headerStr)
            sb.append("\n")
            var divStr = "|"
            headers.forEach { _ ->
                divStr += " ---- | "
            }
            sb.append(divStr)
            sb.append("\n")
        }

        fun addMarkdownTableLine(vararg values: String,sb: java.lang.StringBuilder){
            var headerStr = "|"
            values.forEach {
                headerStr += " $it |"
            }
            sb.append(headerStr)
            sb.append("\n")
        }

    }

}