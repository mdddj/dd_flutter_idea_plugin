package shop.itbug.fluttercheckversionx.tools

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.border.Border


fun emptyBorder(): Border = BorderFactory.createEmptyBorder(0, 0, 0, 0)


/**
 * 统计函数执行时间,返回毫秒
 */
fun measureExecutionTime(block: () -> Unit): Long {
    val startTime = System.currentTimeMillis()  // 获取起始时间
    block()                            // 执行传入的代码块
    val endTime = System.currentTimeMillis()  // 获取结束时间
    return endTime - startTime         // 返回执行时间（毫秒）
}


fun JComponent.showInCenterOfPopup(project: Project) {
    JBPopupFactory.getInstance()
        .createComponentPopupBuilder(this, null).createPopup().showCenteredInCurrentWindow(project)
}

inline fun <reified T : Any> T.log() = logger<T>()
inline val <reified T : Any> T.log get() = logger<T>()