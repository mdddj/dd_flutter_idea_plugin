package shop.itbug.fluttercheckversionx.document

import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

///渲染文档帮助类
class Helper {

    companion object {
        // key - value 格式
        fun addKeyValueSection(key: String, value: String, sb: java.lang.StringBuilder) {
            sb.append("<tr><td class='section'><p>")
            sb.append(key)
            sb.append("</td><td>")
            sb.append("<p>")
            sb.append(value)
            sb.append("</td>")
        }

        //头
        fun addKeyValueHeader(sb: java.lang.StringBuilder) {
            sb.append("<table class='sections'>")
        }

        //尾
        fun addKeyValueFoot(sb: java.lang.StringBuilder) {
            sb.append("</table>")
        }

        // markdown转成html
        fun markdown2Html(markdownText: MyMarkdownDocRenderObject): String {
            return MarkdownRender.markdownToHtml(markdownText)
        }


        fun addMarkdownTableHeader(vararg headers: String, sb: java.lang.StringBuilder) {
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

        /**
         * 把文本设置到剪贴板（复制）
         */
        fun setClipboardString(text: String) {
            CopyPasteManager.getInstance().setContents(StringSelection(text))
        }
    }
}

/**
 * 将string字符串复制到剪贴板
 */
fun String.copyTextToClipboard() {
    Helper.setClipboardString(this)
}