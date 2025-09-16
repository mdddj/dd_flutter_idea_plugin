package shop.itbug.fluttercheckversionx.window.vm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.theme.editorTabStyle
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInstance
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.util.firstChatToUpper
import shop.itbug.fluttercheckversionx.widget.CenterText


@Composable
fun FlutterAppsTabComponent(project: Project, body: @Composable (app: FlutterAppInstance) -> Unit) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val flutterAppList by FlutterXVMService.getInstance(project).runningApps.collectAsState()
    val tabIdList by remember(flutterAppList) { mutableStateOf(flutterAppList.map { it.appInfo.appId }.toList()) }
    val tabs = remember(tabIdList, tabIndex) {
        tabIdList.mapIndexed { index, _ ->
            TabData.Default(
                selected = index == tabIndex,
                content = { tabState ->
                    SimpleTabContent(flutterAppList[index].appInfo.deviceId.firstChatToUpper(), tabState)
                },
                closable = false,
                onClick = {
                    tabIndex = index
                }
            )
        }
    }
    if (flutterAppList.isEmpty()) {
        CenterText(
            """Not Found any running flutter app.
Please make sure the flutter app is running and the observatory is enabled.
If you are running on a real device, please make sure the device is connected to the computer and the port is forwarded.
"""
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TabStrip(
                tabs, JewelTheme.editorTabStyle,
            )
            val selectApp = flutterAppList.getOrNull(tabIndex)
            if (selectApp != null) {
                body.invoke(selectApp)
            }
        }
    }
}