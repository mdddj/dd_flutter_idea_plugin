package shop.itbug.flutterx.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ErrorReportSink
import com.intellij.openapi.diagnostic.UnhandledErrorReport
import com.intellij.openapi.diagnostic.UnhandledExceptionReport
import com.intellij.openapi.diagnostic.UnhandledFreezeReport
import io.sentry.SentryLevel

/**
 * 平台的自动后台上报入口：归属本插件的未处理异常和 UI 冻结会走到这里，
 * 不需要用户点击任何按钮。转发给 [SentryReportingService]。
 *
 * 平台不保证送达、也不去重，所以真正发送前的限流和去重在 service 里做。
 */
class FlutterXErrorReportSink : ErrorReportSink {

    override suspend fun submit(report: UnhandledErrorReport) {
        if (ApplicationManager.getApplication() == null) return
        val service = SentryReportingService.getInstance()
        if (!service.isReporting) return
        when (report) {
            is UnhandledExceptionReport -> {
                val throwable = Throwable().apply { stackTrace = report.stackTrace.toTypedArray() }
                service.captureError(
                    throwable = throwable,
                    message = report.exceptionClass.name,
                    level = SentryLevel.ERROR,
                    tags = mapOf("report.kind" to "unhandled-exception"),
                )
            }

            is UnhandledFreezeReport -> service.captureFreeze(
                durationMs = report.durationMs,
                reason = report.message,
                attachments = report.attachments,
                threadDumps = report.threadDumps.map { it.rawDump },
            )
        }
    }
}
