package shop.itbug.flutterx.common

import codegen.FlutterXPluginInfo
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.NlsActions
import com.intellij.util.system.OS
import com.intellij.util.Consumer
import shop.itbug.flutterx.constance.Links
import shop.itbug.flutterx.document.copyTextToClipboard
import shop.itbug.flutterx.i18n.PluginBundle
import java.awt.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 自定义错误报告提交器。
 *
 * IDE 的 Fatal Errors 对话框点"报告"时调用 [submit]：把完整报告复制到剪贴板，
 * 再打开一个预填好标题和正文的 GitHub issue 页面，由用户确认后提交。
 * 不连接任何后端，也不在后台自动上传。
 */
class PluginErrorHandle(
    /** 打开预填 issue 页面。测试注入假实现，避免真的弹出浏览器。 */
    private val browserLauncher: (String) -> Unit,
) : ErrorReportSubmitter() {

    /** `errorHandler` 扩展点通过无参构造实例化。 */
    constructor() : this({ url -> BrowserUtil.browse(url) })

    private val logger = thisLogger()

    override fun getReportActionText(): @NlsActions.ActionText String =
        PluginBundle.get("error.report.action")

    override fun getPrivacyNoticeText(): String = PluginBundle.get("error.report.privacy")

    override fun submit(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val body = buildReportBody(events, additionalInfo)
        val title = buildIssueTitle(
            FlutterXPluginInfo.NAME,
            headlineOf(events.firstOrNull()?.message, events.firstOrNull()?.throwable)
        )
        // 完整报告始终进剪贴板；URL 里的正文可能被截断
        body.copyTextToClipboard()
        val url = buildIssueUrl(Links.NEW_ISSUE_URL, title, body)
        return try {
            browserLauncher(url)
            consumer.consume(
                SubmittedReportInfo(
                    url,
                    PluginBundle.get("error.report.issue.link"),
                    SubmittedReportInfo.SubmissionStatus.NEW_ISSUE
                )
            )
            true
        } catch (e: Exception) {
            logger.warn("Failed to open the pre-filled GitHub issue page", e)
            false
        }
    }

    internal fun buildReportBody(events: Array<IdeaLoggingEvent>, comment: String?): String {
        val info = ApplicationInfo.getInstance()
        return buildString {
            appendLine("### Description")
            appendLine()
            appendLine(comment?.takeIf { it.isNotBlank() } ?: "_(none provided)_")
            appendLine()
            appendLine("### Environment")
            appendLine()
            appendLine("| Key | Value |")
            appendLine("| --- | --- |")
            appendLine("| Plugin | ${FlutterXPluginInfo.NAME} ${FlutterXPluginInfo.VERSION} |")
            appendLine("| IDE | ${info.fullApplicationName} ${info.fullVersion} |")
            appendLine("| OS | ${OS.CURRENT} ${OS.CURRENT.version()} |")
            events.forEach { event ->
                appendLine()
                appendLine("### Exception")
                appendLine()
                appendLine("~~~text")
                appendLine(event.throwableText.trim().take(MAX_ATTACHMENT_LENGTH))
                appendLine("~~~")
                event.attachments.forEach { attachment ->
                    appendLine()
                    appendLine("**${attachment.name}**")
                    appendLine("~~~text")
                    appendLine(attachment.displayText.trim().take(MAX_ATTACHMENT_LENGTH))
                    appendLine("~~~")
                }
            }
        }
    }
}

/** GitHub issue 正文的字符上限，避免预填 URL 过长被浏览器截断。 */
internal const val MAX_ISSUE_BODY_LENGTH = 6000

/** 报告里单个堆栈/附件的字符上限。 */
private const val MAX_ATTACHMENT_LENGTH = 20_000

/** issue 标题（含插件名前缀）的字符上限。 */
internal const val MAX_ISSUE_TITLE_LENGTH = 120

private const val TRUNCATED_NOTE = "\n\n> Report truncated. The full text has been copied to your clipboard."

/**
 * 生成 issue 标题：折叠换行与连续空白，再按长度截断。
 */
internal fun buildIssueTitle(pluginName: String, headline: String): String {
    val normalized = headline.replace(Regex("\\s+"), " ").trim().ifEmpty { "Unknown error" }
    val raw = "[$pluginName] $normalized"
    return if (raw.length <= MAX_ISSUE_TITLE_LENGTH) raw
    else raw.take(MAX_ISSUE_TITLE_LENGTH - 1) + "…"
}

/**
 * 从日志事件的 message / throwable 推导一行错误摘要。
 */
internal fun headlineOf(message: String?, throwable: Throwable?): String {
    val text = message?.trim()?.takeIf { it.isNotEmpty() }
    if (throwable == null) return text ?: "Unknown error"
    if (text != null && text.startsWith(throwable.javaClass.name)) return text
    val reason = text ?: throwable.message?.trim()?.takeIf { it.isNotEmpty() }
    return if (reason == null) throwable.javaClass.name
    else "${throwable.javaClass.simpleName}: $reason"
}

/**
 * 拼接预填 issue 的 URL。正文按编码后的长度裁剪，避免截断百分号转义。
 */
internal fun buildIssueUrl(newIssueUrl: String, title: String, body: String): String {
    val noteEncoded = encodeForUrl(TRUNCATED_NOTE)
    val (bodyEncoded, complete) = encodeUpTo(body, MAX_ISSUE_BODY_LENGTH - noteEncoded.length)
    val fullBody = if (complete) bodyEncoded else bodyEncoded + noteEncoded
    return "$newIssueUrl?title=${encodeForUrl(title)}&body=$fullBody"
}

private fun encodeForUrl(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

/**
 * 逐码点编码直到预算用尽，返回编码结果和是否完整编码了 [value]。
 */
private fun encodeUpTo(value: String, budget: Int): Pair<String, Boolean> {
    val builder = StringBuilder()
    var index = 0
    while (index < value.length) {
        val codePoint = value.codePointAt(index)
        val chunk = String(Character.toChars(codePoint))
        val encoded = encodeForUrl(chunk)
        if (builder.length + encoded.length > budget) return builder.toString() to false
        builder.append(encoded)
        index += Character.charCount(codePoint)
    }
    return builder.toString() to true
}
