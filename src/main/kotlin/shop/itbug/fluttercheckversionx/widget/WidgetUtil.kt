package shop.itbug.fluttercheckversionx.widget

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory


fun Project.askString(onString: MySimpleTextFieldSubmit) {
    WidgetUtil.getStringWithTextFieldPopup(this, onString)
}

object WidgetUtil {


    fun getStringWithTextFieldPopup(project: Project, onSubmit: MySimpleTextFieldSubmit) {
        getTextEditorPopup("请输入内容", "", { it.showCenteredInCurrentWindow(project) }, onSubmit)
    }

    /**
     * 弹出一个输入框,并获取内容
     */
    fun getTextEditorPopup(
        title: String,
        placeholder: String,
        show: (popup: JBPopup) -> Unit,
        onSubmit: MySimpleTextFieldSubmit,
    ) {
        lateinit var popup: JBPopup
        popup =
            JBPopupFactory.getInstance().createComponentPopupBuilder(MySimpleTextField(placeholder = placeholder) {
                onSubmit.invoke(it)
                popup.cancel()
            }, null)
                .setRequestFocus(true)
                .setTitle(title)
                .setCancelKeyEnabled(true)
                .setResizable(true)
                .setMovable(true)
                .createPopup()
        show.invoke(popup)
    }
}