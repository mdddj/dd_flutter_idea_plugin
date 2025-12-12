package shop.itbug.flutterx.actions.imagePreview.toolbar

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.SearchTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FlutterAssetsPreviewPanelToolbarActionGroup : DefaultActionGroup() {

    companion object {
        fun getActionGroup() = ActionManager.getInstance()
            .getAction("FlutterXAssetsImagePreviewToolBar") as DefaultActionGroup
    }
}


class PreviewSearchTextField(val onChange: (str: String) -> Unit) : SearchTextField(), DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
        handle()
    }

    override fun removeUpdate(e: DocumentEvent?) {
        handle()
    }

    override fun changedUpdate(e: DocumentEvent?) {
        handle()
    }

    private fun handle() {
        onChange.invoke(text)
    }

    init {
        addDocumentListener(this)
    }
}