package shop.itbug.flutterx.common

import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.util.NlsActions

class PluginErrorHandle : ErrorReportSubmitter() {
    override fun getReportActionText(): @NlsActions.ActionText String {
        return "报告给梁典典"
    }

}