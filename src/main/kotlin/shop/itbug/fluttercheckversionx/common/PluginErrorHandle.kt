package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.util.NlsActions

class PluginErrorHandle : ErrorReportSubmitter() {
    override fun getReportActionText(): @NlsActions.ActionText String {
        return "FlutterX Submit Error"
    }
    
}