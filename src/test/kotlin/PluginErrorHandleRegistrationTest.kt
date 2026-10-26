import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import shop.itbug.flutterx.common.PluginErrorHandle

/**
 * 校验 `errorHandler` 注册能被平台读到，且文案真的从语言包解析出来。
 * 如果 plugin.xml 里没有注册，IDE 的 Fatal Errors 对话框不会出现"反馈到 GitHub"；
 * 如果缺少语言包 key，`PluginBundle` 会原样返回 key 名。
 */
class PluginErrorHandleRegistrationTest : BasePlatformTestCase() {

    fun testSubmitterIsRegisteredForThisPlugin() {
        val mine = ErrorReportSubmitter.EP_NAME.extensionList.filterIsInstance<PluginErrorHandle>()

        assertEquals(1, mine.size)
        assertEquals("shop.itbug.FlutterCheckVersionX", mine.single().pluginDescriptor.pluginId.idString)
    }

    fun testTextsComeFromBundle() {
        val submitter = PluginErrorHandle()

        assertFalse(submitter.reportActionText.isBlank())
        assertFalse(
            "missing bundle key",
            submitter.reportActionText.startsWith("error.report.")
        )
        assertFalse(
            "missing bundle key",
            submitter.privacyNoticeText.startsWith("error.report.")
        )
        // 数据流向必须写清楚：GitHub issue + 剪贴板
        assertTrue(submitter.privacyNoticeText.contains("GitHub"))
        assertTrue(submitter.privacyNoticeText.length > 40)
    }

    fun testReportBodyCarriesEnvironmentAndAttachments() {
        val event = IdeaLoggingEvent(
            "Something broke",
            IllegalStateException("boom"),
            listOf(Attachment("details.txt", "attachment payload")),
            null as com.intellij.openapi.diagnostic.ProblematicPluginInfo?,
            null
        )

        val body = PluginErrorHandle().buildReportBody(arrayOf(event), "user comment")

        assertTrue(body.contains("user comment"))
        assertTrue(body.contains("FlutterX 7.3.2"))
        assertTrue(body.contains("IllegalStateException: boom"))
        assertTrue(body.contains("attachment payload"))
        assertTrue(body.contains("details.txt"))
    }

    fun testSubmitOpensPrefilledIssueAndReportsNewIssue() {
        var opened: String? = null
        var result: SubmittedReportInfo? = null
        val submitter = PluginErrorHandle { url -> opened = url }
        val event = IdeaLoggingEvent("Something broke", IllegalStateException("boom"))

        val accepted = submitter.submit(
            arrayOf(event),
            "user comment",
            java.awt.Canvas(),
            Consumer { result = it }
        )

        assertTrue(accepted)
        assertNotNull(opened)
        assertTrue(opened!!.startsWith("https://github.com/mdddj/dd_flutter_idea_plugin/issues/new?"))
        assertTrue(opened!!.contains("title="))
        assertTrue(opened!!.contains("body="))
        assertEquals(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, result?.status)
        assertEquals(opened, result?.url)
    }

    fun testSubmitReturnsFalseWhenBrowserFails() {
        var consumed = false
        val submitter = PluginErrorHandle { throw IllegalStateException("no browser") }

        val accepted = submitter.submit(
            arrayOf(IdeaLoggingEvent("x", IllegalStateException("boom"))),
            null,
            java.awt.Canvas(),
            Consumer { consumed = true }
        )

        assertFalse(accepted)
        assertFalse("callback must not be completed when submission never starts", consumed)
    }
}
