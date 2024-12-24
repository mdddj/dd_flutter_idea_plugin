package shop.itbug.fluttercheckversionx.document

import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil.appendStyledSpan
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.lang.dart.DartLanguage
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/**
 * markdown节点
 * kDocTag Link: [https://github.com/JetBrains/kotlin/blob/master/compiler/psi/src/org/jetbrains/kotlin/kdoc/psi/impl/KDocTag.kt]
 */
class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val comment: MyMarkdownDocRenderObject) {
    val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, comment) }
    private val endOffset: Int get() = node.endOffset
    private val startOffset: Int get() = node.startOffset
    val type: IElementType get() = node.type
    val text: String get() = comment.getContent().substring(startOffset, endOffset)
    fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }
}

class MarkdownRender {


    companion object {

        fun StringBuilder.appendTag(tag: MyMarkdownDocRenderObject?, title: String) {
            if (tag != null) {
                appendSection(title) {
                    val m = markdownToHtml(tag)
                    append(m)
                }
            }
        }

        private fun StringBuilder.appendSection(title: String, content: StringBuilder.() -> Unit) {
            append(SECTION_HEADER_START, title, ":", SECTION_SEPARATOR)
            content()
            append(SECTION_END)
        }

        fun markdownToHtml(comment: MyMarkdownDocRenderObject, allowSingleParagraph: Boolean = true): String {
            val markdownTree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(comment.getContent())
            val markdownNode = MarkdownNode(markdownTree, null, comment)
            val maybeSingleParagraph = markdownNode.children.singleOrNull { it.type != MarkdownTokenTypes.EOL }
            val firstParagraphOmitted = when {
                maybeSingleParagraph != null && !allowSingleParagraph -> {
                    maybeSingleParagraph.children.joinToString("") { if (it.text == "\n") " " else it.toHtml(comment.project) }
                }

                else -> markdownNode.toHtml(comment.project)
            }

            val topMarginOmitted = when {
                firstParagraphOmitted.startsWith("<p>") -> firstParagraphOmitted.replaceFirst(
                    "<p>",
                    "<p style='margin-top:0;padding-top:0;'>"
                )

                else -> firstParagraphOmitted
            }

            return topMarginOmitted
        }


    }
}

///处理表格行
private fun processTableRow(
    sb: StringBuilder,
    node: MarkdownNode,
    cellTag: String,
    alignment: List<String>,
    project: Project
) {
    sb.append("<tr style=\"${if (cellTag == "th") "" else ""}\">")
    for ((i, child) in node.children.filter { it.type == GFMTokenTypes.CELL }.withIndex()) {
        val alignValue = alignment.getOrElse(i) { "" }
        val alignTag = if (alignValue.isEmpty()) "" else " align=\"$alignValue\" "
        sb.append("<$cellTag$alignTag style=\"padding: 2px 8px;white-space: nowrap;margin:1px;${if (cellTag == "td") "" else ""}\">")
        sb.append("<code>${child.toHtml(project)}</code>")
        sb.append("</$cellTag>")
    }
    sb.append("</tr>")
}

/**
 * markdown节点转成html
 */
fun MarkdownNode.toHtml(project: Project): String {


    if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
        return text   // do not trim trailing whitespace
    }

    var currentCodeFenceLang = "dart"
    val lang = guessLanguage(currentCodeFenceLang) ?: DartLanguage.INSTANCE
    val sb = StringBuilder()
    visit { node, processChildren ->


        fun wrapChildren(tag: String, newline: Boolean = false) {
            sb.append("<$tag>")
            processChildren()
            sb.append("</$tag>")
            if (newline) sb.appendLine()
        }

        val nodeType = node.type
        var nodeText = node.text


        if (nodeText.contains("{@tool snippet}")) {
            nodeText = nodeText.replace("{@tool snippet}", "<pre>")
        }
        if (nodeText.contains("{@end-tool}")) {
            nodeText = nodeText.replace("{@end-tool}", "</pre>")
        }

        println("\nnode text: $nodeText   ✅NodeType=$nodeType \n")

        when (nodeType) {
            MarkdownElementTypes.UNORDERED_LIST -> wrapChildren("ul", newline = true)
            MarkdownElementTypes.ORDERED_LIST -> wrapChildren("ol", newline = true)
            MarkdownElementTypes.LIST_ITEM -> wrapChildren("li")
            MarkdownElementTypes.EMPH -> wrapChildren("em")
            MarkdownElementTypes.STRONG -> wrapChildren("strong")
            GFMElementTypes.STRIKETHROUGH -> wrapChildren("del")
            MarkdownElementTypes.ATX_1 -> wrapChildren("h1")
            MarkdownElementTypes.ATX_2 -> wrapChildren("h2")
            MarkdownElementTypes.ATX_3 -> wrapChildren("h3")
            MarkdownElementTypes.ATX_4 -> wrapChildren("h4")
            MarkdownElementTypes.ATX_5 -> wrapChildren("h5")
            MarkdownElementTypes.ATX_6 -> wrapChildren("h6")
            MarkdownElementTypes.BLOCK_QUOTE -> wrapChildren("blockquote")
            MarkdownElementTypes.PARAGRAPH -> {
                sb.trimEnd()
                wrapChildren("p", newline = true)
            }

            MarkdownElementTypes.CODE_SPAN -> {
                val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                if (startDelimiter != null) {
                    val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                    sb.append(HtmlChunk.tag("code").addText(text))
                } else {
                    sb.append(HtmlChunk.tag("code").addText(nodeText))
                }
            }

            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.CODE_FENCE -> {
                sb.append("<div>")
                processChildren()
                sb.append("</div>")
            }

            MarkdownTokenTypes.FENCE_LANG -> {
                currentCodeFenceLang = nodeText
//                sb.append("<div style='position:relative;right:12;top:12;' >$nodeText</div>")
            }

            MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
                if (node.text.startsWith("[") && node.text.endsWith("]")) {
                    val r = node.text.removeSuffix("]").removePrefix("[")
                    DocumentationManagerUtil.createHyperlink(sb, r, r, false)
                } else {
                    sb.append(node.text)
                }
            }

            MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                val linkLabelNode = node.child(MarkdownElementTypes.LINK_LABEL)
                val linkLabelContent = linkLabelNode?.children
                    ?.dropWhile { it.type == MarkdownTokenTypes.LBRACKET }
                    ?.dropLastWhile { it.type == MarkdownTokenTypes.RBRACKET }
                if (linkLabelContent != null) {
                    val label = linkLabelContent.joinToString(separator = "") { it.text }
                    val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml(project) ?: label
                    if (DumbService.isDumb(comment.project)) {
                        sb.append(linkText)
                    } else {
                        wrapChildren("strong")
                    }
                } else {
                    sb.append(node.text)
                }
            }

            MarkdownElementTypes.INLINE_LINK -> {
                val label = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml(project)
                val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                val videoHtmlText = generateFlutterMp4Preview(destination ?: "")
                if (videoHtmlText != null) {
                    sb.append(videoHtmlText)
                } else {
                    if (label != null && destination != null) {
                        val atag = HtmlChunk.tag("a").attr("href", destination).addText(label).toString()
                        sb.append(atag)
                    } else {
                        sb.append(node.text)
                    }
                }

            }

            MarkdownTokenTypes.TEXT,
            MarkdownTokenTypes.WHITE_SPACE,
            MarkdownTokenTypes.COLON,
            MarkdownTokenTypes.SINGLE_QUOTE,
            MarkdownTokenTypes.DOUBLE_QUOTE,
            MarkdownTokenTypes.LPAREN,
            MarkdownTokenTypes.RPAREN,
            MarkdownTokenTypes.LBRACKET,
            MarkdownTokenTypes.RBRACKET,
            MarkdownTokenTypes.EXCLAMATION_MARK,
            GFMTokenTypes.CHECK_BOX,
            GFMTokenTypes.GFM_AUTOLINK -> {
                sb.append(nodeText)
            }

            MarkdownTokenTypes.CODE_FENCE_START -> {
                sb.append("<pre style='padding-bottom:0px;'>")
            }

            MarkdownTokenTypes.CODE_FENCE_END -> {
                sb.append("</pre>")
            }

            MarkdownTokenTypes.CODE_LINE,
            MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                sb.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                    comment.project,
                    lang,
                    nodeText
                )
            }

            MarkdownTokenTypes.EOL -> {
                val parentType = node.parent?.type
                if (parentType == MarkdownElementTypes.CODE_BLOCK || parentType == MarkdownElementTypes.CODE_FENCE) {
                    sb.append("\n")
                } else {
                    sb.append(" ")
                }
            }

            MarkdownTokenTypes.GT -> sb.append("&gt;")
            MarkdownTokenTypes.LT -> sb.append("&lt;")

            MarkdownElementTypes.LINK_TEXT -> {
                val childrenWithoutBrackets = node.children.drop(1).dropLast(1)
                for (child in childrenWithoutBrackets) {
                    sb.append(child.toHtml(project))
                }
            }

            MarkdownTokenTypes.EMPH -> {
                val parentNodeType = node.parent?.type
                if (parentNodeType != MarkdownElementTypes.EMPH && parentNodeType != MarkdownElementTypes.STRONG) {
                    sb.append(node.text)
                }
            }

            GFMTokenTypes.TILDE -> {
                if (node.parent?.type != GFMElementTypes.STRIKETHROUGH) {
                    sb.append(node.text)
                }
            }

            GFMElementTypes.TABLE -> {
                val alignment: List<String> = getTableAlignment(node)
                var addedBody = false
                sb.append("<table style='width:100%;color:#adb5bd'>")

                for (child in node.children) {
                    if (child.type == GFMElementTypes.HEADER) {
                        sb.append("<thead>")
                        processTableRow(sb, child, "th", alignment, project)
                        sb.append("</thead>")
                    } else if (child.type == GFMElementTypes.ROW) {
                        if (!addedBody) {
                            sb.append("<tbody>")
                            addedBody = true
                        }

                        processTableRow(sb, child, "td", alignment, project)
                    }
                }

                if (addedBody) {
                    sb.append("</tbody>")
                }
                sb.append("</table>")
            }

            else -> {
                processChildren()
            }
        }
    }
    val fullHtml = sb.toString().trimEnd()
    return fullHtml
}

///解析mp4链接
fun generateFlutterMp4Preview(url: String): String? {
    if (url.endsWith(".mp4") && url.startsWith("https://flutter.github.io")) {
        val html = HtmlChunk.div().child(HtmlChunk.tag("a").attr("href", url).addText(url)).toString()
        return html
    }
    return null
}

/**
 * 检索节点
 */
private fun MarkdownNode.visit(action: (MarkdownNode, () -> Unit) -> Unit) {
    action(this) {
        for (child in children) {
            child.visit(action)
        }
    }
}


private fun getTableAlignment(node: MarkdownNode): List<String> {
    val separatorRow = node.child(GFMTokenTypes.TABLE_SEPARATOR)
        ?: return emptyList()

    return separatorRow.text.split('|').filterNot { it.isBlank() }.map {
        val trimmed = it.trim()
        val left = trimmed.startsWith(':')
        val right = trimmed.endsWith(':')
        if (left && right) "center"
        else if (right) "right"
        else if (left) "left"
        else ""
    }
}

/**
 * 文字代码高亮
 */
private fun StringBuilder.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
    project: Project,
    language: Language,
    codeSnippet: String
): StringBuilder {
    val codeSnippetBuilder = StringBuilder()
    HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
        codeSnippetBuilder,
        project,
        language,
        codeSnippet,
        false,
        DocumentationSettings.getHighlightingSaturation(true)
    )
    val codeAttributes =
        EditorColorsManager.getInstance().globalScheme.getAttributes(TextAttributesKey.createTextAttributesKey("DART"))
            .clone()
    codeAttributes.backgroundColor = null
    appendStyledSpan(true, codeAttributes, codeSnippetBuilder.toString())
    return this
}


private fun StringBuilder.appendStyledSpan(
    doHighlighting: Boolean,
    attributes: TextAttributes,
    value: String?
): StringBuilder {
    if (doHighlighting) {
        appendStyledSpan(this, attributes, value, DocumentationSettings.getHighlightingSaturation(true))
    } else {
        append(value)
    }
    return this
}

//分析代码语言
private fun guessLanguage(name: String): Language? {
    val lower = StringUtil.toLowerCase(name)
    return Language.findLanguageByID(lower)
        ?: Language.getRegisteredLanguages().firstOrNull { StringUtil.toLowerCase(it.id) == lower }
}