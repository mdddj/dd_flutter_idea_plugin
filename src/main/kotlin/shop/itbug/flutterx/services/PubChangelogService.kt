package shop.itbug.flutterx.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.io.HttpRequests
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import shop.itbug.flutterx.config.DioListingUiConfig

data class PubChangelogEntry(
    val version: String,
    val content: String,
) {
    val formattedText: String
        get() = buildString {
            append(version)
            if (content.isNotBlank()) {
                append('\n')
                append(content)
            }
        }
}

class PubChangelogParser {

    fun parseLatestChangelog(html: String): PubChangelogEntry? {
        val document = Jsoup.parse(html)
        val container = document.selectFirst("section.detail-tab-changelog-content")
            ?: document.selectFirst(".detail-tab-changelog-content")
            ?: document.selectFirst(".markdown-body")
            ?: return null

        val entry = container.selectFirst(".changelog-entry")
        if (entry != null) {
            return parseEntry(entry)
        }

        return parseFallback(container)
    }

    private fun parseEntry(entry: Element): PubChangelogEntry? {
        val version = entry.selectFirst("h2.changelog-version")
            ?.ownText()
            ?.normalizeWhitespace()
            ?.takeIf { it.isNotEmpty() }
            ?: entry.selectFirst("h1, h2, h3, h4")
                ?.ownText()
                ?.normalizeWhitespace()
                ?.takeIf { it.isNotEmpty() }
            ?: return null

        val contentElement = entry.selectFirst(".changelog-content")
        val content = when {
            contentElement != null -> renderNodes(contentElement.childNodes(), quoteLevel = 0)
            else -> renderNodes(childNodesAfterFirstHeading(entry), quoteLevel = 0)
        }
        return PubChangelogEntry(version = version, content = content)
    }

    private fun parseFallback(container: Element): PubChangelogEntry? {
        val heading = container.selectFirst("h1, h2, h3, h4") ?: return null
        val version = heading.ownText().normalizeWhitespace().ifEmpty {
            heading.text().normalizeWhitespace()
        }
        if (version.isEmpty()) return null

        val contentNodes = mutableListOf<Node>()
        var sibling = heading.nextSibling()
        while (sibling != null) {
            val stop = sibling is Element && sibling.tagName().matches(Regex("h[1-4]", RegexOption.IGNORE_CASE))
            if (stop) break
            contentNodes += sibling
            sibling = sibling.nextSibling()
        }
        val content = renderNodes(contentNodes, quoteLevel = 0)
        return PubChangelogEntry(version = version, content = content)
    }

    private fun childNodesAfterFirstHeading(entry: Element): List<Node> {
        val firstHeadingIndex = entry.childNodes().indexOfFirst { node ->
            node is Element && node.tagName().matches(Regex("h[1-4]", RegexOption.IGNORE_CASE))
        }
        if (firstHeadingIndex < 0) return entry.childNodes()
        return entry.childNodes().drop(firstHeadingIndex + 1)
    }

    private fun renderNodes(nodes: List<Node>, quoteLevel: Int): String {
        val lines = mutableListOf<String>()
        nodes.forEach { appendNode(it, lines, quoteLevel) }
        return collapseBlankLines(lines)
    }

    private fun appendNode(node: Node, lines: MutableList<String>, quoteLevel: Int) {
        when (node) {
            is TextNode -> {
                val text = node.text().normalizeWhitespace()
                if (text.isNotEmpty()) {
                    lines += "${quotePrefix(quoteLevel)}$text"
                    appendSpacer(lines)
                }
            }

            is Element -> appendBlock(node, lines, quoteLevel)
        }
    }

    private fun appendBlock(element: Element, lines: MutableList<String>, quoteLevel: Int) {
        when (element.tagName().lowercase()) {
            "blockquote" -> {
                element.childNodes().forEach { appendNode(it, lines, quoteLevel + 1) }
            }

            "ul", "ol" -> {
                element.children()
                    .filter { it.tagName().equals("li", ignoreCase = true) }
                    .forEach { li ->
                        val text = li.text().normalizeWhitespace()
                        if (text.isNotEmpty()) {
                            lines += "${quotePrefix(quoteLevel)}- $text"
                        }
                    }
                appendSpacer(lines)
            }

            "p" -> {
                val text = element.text().normalizeWhitespace()
                if (text.isNotEmpty()) {
                    lines += "${quotePrefix(quoteLevel)}$text"
                    appendSpacer(lines)
                }
            }

            "pre" -> {
                val text = element.text().trimEnd()
                if (text.isNotEmpty()) {
                    lines += "${quotePrefix(quoteLevel)}```"
                    text.lineSequence().forEach { line ->
                        lines += "${quotePrefix(quoteLevel)}$line"
                    }
                    lines += "${quotePrefix(quoteLevel)}```"
                    appendSpacer(lines)
                }
            }

            "table" -> {
                element.select("tr").forEach { row ->
                    val columns = row.select("th, td").map { it.text().normalizeWhitespace() }
                    if (columns.isNotEmpty()) {
                        lines += "${quotePrefix(quoteLevel)}| ${columns.joinToString(" | ")} |"
                    }
                }
                appendSpacer(lines)
            }

            else -> {
                if (element.childNodeSize() > 0 && element.children().isNotEmpty()) {
                    element.childNodes().forEach { appendNode(it, lines, quoteLevel) }
                } else {
                    val text = element.text().normalizeWhitespace()
                    if (text.isNotEmpty()) {
                        lines += "${quotePrefix(quoteLevel)}$text"
                        appendSpacer(lines)
                    }
                }
            }
        }
    }

    private fun appendSpacer(lines: MutableList<String>) {
        if (lines.isNotEmpty() && lines.last().isNotBlank()) {
            lines += ""
        }
    }

    private fun quotePrefix(quoteLevel: Int): String {
        if (quoteLevel <= 0) return ""
        return buildString {
            repeat(quoteLevel) {
                append("> ")
            }
        }
    }

    private fun collapseBlankLines(lines: List<String>): String {
        val result = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank()) {
                if (result.isNotEmpty() && result.last().isNotBlank()) {
                    result += ""
                }
            } else {
                result += line
            }
        }

        while (result.lastOrNull()?.isBlank() == true) {
            result.removeLast()
        }
        return result.joinToString("\n")
    }

    private fun String.normalizeWhitespace(): String = trim().replace(Regex("\\s+"), " ")
}

object PubChangelogService {
    private val logger = thisLogger()
    private val parser = PubChangelogParser()

    fun getChangelogUrl(packageName: String): String {
        val baseUrl = DioListingUiConfig.setting.pubServerUrl.trimEnd('/')
        return "$baseUrl/packages/$packageName/changelog"
    }

    fun fetchLatestChangelog(packageName: String): PubChangelogEntry? {
        val url = getChangelogUrl(packageName)
        return try {
            val html = HttpRequests.request(url)
                .connectTimeout(10_000)
                .readTimeout(20_000)
                .readString()
            parser.parseLatestChangelog(html)
        } catch (e: Exception) {
            logger.warn("Failed to fetch package changelog for $packageName from $url", e)
            null
        }
    }
}
