package note.setting

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import note.jdbc.SqliteConnectManager
import shop.itbug.fluttercheckversionx.common.MyDialogWrapper
import javax.swing.JComponent


///设置flutter plugin 收藏面板
class InitFlutterPluginSettingDialog( override val project: Project) : MyDialogWrapper(project) {

    init {
        super.init()
        title = "插件收藏"

    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row ("状态") {
                button("初始化表") {
                    SqliteConnectManager.createFlutterPluginTable()
                }
            }
        }
    }

}