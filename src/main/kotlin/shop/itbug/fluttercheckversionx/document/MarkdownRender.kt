package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.Language
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.lang.dart.DartLanguage
import com.siyeh.ig.ui.UiUtils
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
 */
class MarkdownNode(val node: ASTNode, val parent: MarkdownNode?, val comment: String) {
    val children: List<MarkdownNode> = node.children.map { MarkdownNode(it, this, comment) }
    val endOffset: Int get() = node.endOffset
    val startOffset: Int get() = node.startOffset
    val type: IElementType get() = node.type
    val text: String get() = comment.substring(startOffset, endOffset)
    fun child(type: IElementType): MarkdownNode? = children.firstOrNull { it.type == type }
}

class MarkdownRender {


    companion object {


        /**
         * 处理dart注释
         */
        fun renderText(comment: String, project: Project): String {
            val markdownTree =
                MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(comment)
            val markdownNode = MarkdownNode(markdownTree, null, comment)


            val maybeSingleParagraph = markdownNode.children.singleOrNull { it.type != MarkdownTokenTypes.EOL }


            val firstParagraphOmitted = when {
                maybeSingleParagraph?.type == GFMElementTypes.TABLE -> {
                    return maybeSingleParagraph.toHtml(project)
                }
                maybeSingleParagraph != null -> {
                    maybeSingleParagraph.children.joinToString("") {
                        if (it.text == "\n") {
                            ""
                        } else it.toHtml(project)
                    }
                }
                else -> markdownNode.toHtml(project)
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
    val bg = UIUtil.colorToHex(UIUtil.getTableBackground())
    sb.append("<tr style=\"${if (cellTag == "th") "background-color: $bg" else "background-color: $bg"}\">")
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


    ///不要删除尾随空格
//    if (node.type == MarkdownTokenTypes.WHITE_SPACE) {
//        return text
//    }


    /// 当前项目的主要语言
    var currentCodeFenceLang = "dart"

    /// 字符串构建器
    val sb = StringBuilder()

    ///遍历节点,单独处理
    visit { node, processChildren ->

        ///包裹子元素
        fun wrapChildren(tag: String, newline: Boolean = false) {
            sb.append("<$tag>")
            processChildren()
            sb.append("</$tag>")
            if (newline) sb.appendLine()
        }

        /// 节点类型
        val nodeType = node.type

        ///节点的文本内容
        val nodeText = node.text

        println("$nodeType -> $nodeText")

        //对节点的每一项单独处理
        when (nodeType) {

            //无符号列表
            MarkdownElementTypes.UNORDERED_LIST -> {
                wrapChildren("ul", newline = true)
            }

            //顺序列表
            MarkdownElementTypes.ORDERED_LIST -> wrapChildren("ol", newline = true)

            //列表项
            MarkdownElementTypes.LIST_ITEM -> wrapChildren("li")

            //斜体
            MarkdownElementTypes.EMPH -> wrapChildren("em")

            //粗体
            MarkdownElementTypes.STRONG -> wrapChildren("strong")

            //删除
            GFMElementTypes.STRIKETHROUGH -> wrapChildren("del")

            //标题1
            MarkdownElementTypes.ATX_1 -> wrapChildren("h1")

            //标题2
            MarkdownElementTypes.ATX_2 -> wrapChildren("h2")

            //标题3
            MarkdownElementTypes.ATX_3 -> wrapChildren("h3")

            //标题4
            MarkdownElementTypes.ATX_4 -> wrapChildren("h4")

            //标题5
            MarkdownElementTypes.ATX_5 -> wrapChildren("h5")

            //标题6
            MarkdownElementTypes.ATX_6 -> wrapChildren("h6")

            //笔记类型
            MarkdownElementTypes.BLOCK_QUOTE -> wrapChildren("blockquote")

            //图片
            MarkdownElementTypes.IMAGE -> {
                val text = node.text
                val url = text.substring(text.indexOf("(") + 1, text.indexOf(")"))
                println(url)
                sb.append("<img src=\"$url\" />")
                sb.appendLine()
            }


            // 文档注释里面的变量
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> sb.append("<b><code>$nodeText</code></b>")

            //段落p类型
            MarkdownElementTypes.PARAGRAPH -> {
                sb.trimEnd()
                wrapChildren("p", newline = true)
            }

            //代码类型
            MarkdownElementTypes.CODE_SPAN -> {
                val startDelimiter = node.child(MarkdownTokenTypes.BACKTICK)?.text
                if (startDelimiter != null) {
                    val text = node.text.substring(startDelimiter.length).removeSuffix(startDelimiter)
                    sb.append("<code style='font-size:${DocumentationSettings.getMonospaceFontSizeCorrection(true)}%;'>")
                    sb.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                        DocumentationSettings.getInlineCodeHighlightingMode(),
                        project,
                        DartLanguage.INSTANCE,
                        text
                    )
                    sb.append("</code>")
                }
            }

            //内联代码块
            MarkdownElementTypes.CODE_BLOCK,
            MarkdownElementTypes.CODE_FENCE -> {
                sb.trimEnd()
                sb.append("<pre><code style='font-size:${DocumentationSettings.getMonospaceFontSizeCorrection(true)}%;'>")
                processChildren()
                sb.append("</code></pre>")
            }


            MarkdownTokenTypes.FENCE_LANG -> {
                currentCodeFenceLang = nodeText
            }

            //长短链接
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK -> {
                val linkLabelNode = node.child(MarkdownElementTypes.LINK_LABEL)
                val linkLabelContent = linkLabelNode?.children
                    ?.dropWhile { it.type == MarkdownTokenTypes.LBRACKET }
                    ?.dropLastWhile { it.type == MarkdownTokenTypes.RBRACKET }
                if (linkLabelContent != null) {
                    val label = linkLabelContent.joinToString(separator = "") { it.text }
                    val linkText = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml(project) ?: label
                    if (DumbService.isDumb(project)) {
                        sb.append(linkText)
                    }
                } else {
                    sb.append(node.text)
                }
            }
            MarkdownElementTypes.INLINE_LINK -> {
                val label = node.child(MarkdownElementTypes.LINK_TEXT)?.toHtml(project)
                val destination = node.child(MarkdownElementTypes.LINK_DESTINATION)?.text
                if (label != null && destination != null) {
                    sb.append("<a href=\"$destination\">$label</a>")
                } else {
                    sb.append(node.text)
                }
            }
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
            MarkdownTokenTypes.TEXT -> {
                sb.append(nodeText)
            }
            MarkdownTokenTypes.AUTOLINK -> {
                sb.append("<a href=\"$nodeText\">$nodeText</a>")
            }
            MarkdownTokenTypes.CODE_LINE,
            MarkdownTokenTypes.CODE_FENCE_CONTENT -> {
                sb.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                    when (DocumentationSettings.isHighlightingOfCodeBlocksEnabled()) {
                        true -> DocumentationSettings.InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING
                        false -> DocumentationSettings.InlineCodeHighlightingMode.NO_HIGHLIGHTING
                    },
                    project,
                    guessLanguage(currentCodeFenceLang) ?: DartLanguage.INSTANCE,
                    nodeText
                )
            }
            MarkdownTokenTypes.EOL -> {
                val parentType = node.parent?.type
                if (parentType == MarkdownElementTypes.CODE_BLOCK || parentType == MarkdownElementTypes.CODE_FENCE) {
                    sb.append("\n")
                } else {
                    sb.append("<div style=\"height: 4px\"/>")
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
                sb.append("<table>")

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

                        //处理表格行
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

    return sb.toString().trimEnd()
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

private fun StringBuilder.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
    highlightingMode: DocumentationSettings.InlineCodeHighlightingMode,
    project: Project,
    language: Language,
    codeSnippet: String
): StringBuilder {
    val codeSnippetBuilder = StringBuilder()
    if (highlightingMode == DocumentationSettings.InlineCodeHighlightingMode.SEMANTIC_HIGHLIGHTING) { // highlight code by lexer
        HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
            codeSnippetBuilder,
            project,
            language,
            codeSnippet,
            false,
            DocumentationSettings.getHighlightingSaturation(true)
        )
    } else {
        codeSnippetBuilder.append(StringUtil.escapeXmlEntities(codeSnippet))
    }
    if (highlightingMode != DocumentationSettings.InlineCodeHighlightingMode.NO_HIGHLIGHTING) {
        // set code text color as editor default code color instead of doc component text color
        val codeAttributes =
            EditorColorsManager.getInstance().globalScheme.getAttributes(HighlighterColors.TEXT).clone()
        codeAttributes.backgroundColor = null
        appendStyledSpan(true, codeAttributes, codeSnippetBuilder.toString())
    } else {
        append(codeSnippetBuilder.toString())
    }
    return this
}

private fun StringBuilder.appendStyledSpan(
    doHighlighting: Boolean,
    attributes: TextAttributes,
    value: String?
): StringBuilder {
    if (doHighlighting) {
        HtmlSyntaxInfoUtil.appendStyledSpan(
            this,
            attributes,
            value,
            DocumentationSettings.getHighlightingSaturation(true)
        )
    } else {
        append(value)
    }
    return this
}

private fun guessLanguage(name: String): Language? {
    val lower = StringUtil.toLowerCase(name)
    return Language.findLanguageByID(lower)
        ?: Language.getRegisteredLanguages().firstOrNull { StringUtil.toLowerCase(it.id) == lower }
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