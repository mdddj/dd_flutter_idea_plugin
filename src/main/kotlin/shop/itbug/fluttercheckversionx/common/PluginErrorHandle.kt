package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.util.NlsActions
import com.intellij.util.Consumer
import java.awt.Component

class PluginErrorHandle : ErrorReportSubmitter() {
    override fun getReportActionText(): @NlsActions.ActionText String {
        return "FlutterX Submit Error"
    }

    override fun submit(
        events: Array<out IdeaLoggingEvent?>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>
    ): Boolean {


        return true
    }
}