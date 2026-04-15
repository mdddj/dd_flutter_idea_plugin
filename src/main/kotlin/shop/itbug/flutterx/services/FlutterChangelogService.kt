package shop.itbug.flutterx.services

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.io.HttpRequests
import shop.itbug.flutterx.tools.FlutterVersionTool

data class FlutterChangelogEntry(
    val version: String,
    val items: List<String>,
) {
    val summaryText: String
        get() = items.joinToString("\n")
}

class FlutterChangelogMarkdownParser {

    fun parseVersion(markdown: String, version: String): FlutterChangelogEntry? {
        val normalized = markdown.replace("\r\n", "\n")
        val section = extractVersionSection(normalized, version) ?: return null
        val items = parseItems(section)
        if (items.isEmpty()) return null
        return FlutterChangelogEntry(version = version, items = items)
    }

    private fun extractVersionSection(markdown: String, version: String): String? {
        val escapedVersion = Regex.escape(version)
        val headingPattern = Regex(
            """(?m)^###\s+(?:\[$escapedVersion]\([^)]+\)|$escapedVersion)\s*$"""
        )
        val start = headingPattern.find(markdown) ?: return null
        val contentStart = start.range.last + 1
        val rest = markdown.substring(contentStart)
        val nextHeading = Regex("""(?m)^(?:###|##)\s+""").find(rest)
        val contentEnd = nextHeading?.range?.first ?: rest.length
        return rest.substring(0, contentEnd).trim()
    }

    private fun parseItems(section: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var currentIsBullet = false

        fun flush() {
            val text = current.toString().trim().normalizeMarkdownText()
            if (text.isNotEmpty()) {
                result += text
            }
            current.clear()
            currentIsBullet = false
        }

        section.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> flush()
                line.startsWith("- ") -> {
                    flush()
                    current.append(line.removePrefix("- ").trim())
                    currentIsBullet = true
                }

                current.isEmpty() -> {
                    current.append(line.trim())
                }

                currentIsBullet -> {
                    current.append(' ').append(line.trim())
                }

                else -> {
                    current.append(' ').append(line.trim())
                }
            }
        }
        flush()
        return result
    }

    private fun String.normalizeMarkdownText(): String {
        return this
            .replace(Regex("""\[(.+?)]\((.+?)\)"""), "$1")
            .replace("`", "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}

object FlutterChangelogService {
    private val logger = thisLogger()
    private val parser = FlutterChangelogMarkdownParser()

    fun fetchVersionChangelog(version: String): FlutterChangelogEntry? {
        val url = FlutterVersionTool.buildChangeLogRawUrl(version)
        return try {
            val markdown = HttpRequests.request(url)
                .connectTimeout(10_000)
                .readTimeout(20_000)
                .readString()
            parser.parseVersion(markdown, version)
        } catch (e: Exception) {
            logger.warn("Failed to fetch Flutter changelog for version $version from $url", e)
            null
        }
    }
}
