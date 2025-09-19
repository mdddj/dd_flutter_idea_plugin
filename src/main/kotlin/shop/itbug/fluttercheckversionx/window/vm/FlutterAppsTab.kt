package shop.itbug.fluttercheckversionx.window.vm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.theme.editorTabStyle
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInstance
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.config.PluginConfig
import shop.itbug.fluttercheckversionx.constance.discordUrl
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.firstChatToUpper
import shop.itbug.fluttercheckversionx.widget.CenterText
import java.net.URI


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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column {
                CenterText(
                    """Not Found any running flutter app.
Please make sure the flutter app is running and the observatory is enabled.
If you are running on a real device, please make sure the device is connected to the computer and the port is forwarded.
"""
                )
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Link(PluginBundle.get("doc"), onClick = {
                        BrowserUtil.browse(URI.create("https://flutterx.itbug.shop/en/vm/dart_vm_panel/"))
                    })
                    if(PluginConfig.getState(project).showDiscord){
                        Link("Discord",onClick = {
                            BrowserUtil.browse(discordUrl)
                        })
                    }

                    if (PluginConfig.getState(project).showRewardAction) {
                        Link(PluginBundle.get("reward") + "(wechat)", onClick = {
                            BrowserUtil.browse(URI.create("https://itbug.shop/static/ds.68eb4cac.jpg"))
                        })
                    }

                    Link(PluginBundle.get("bugs"), onClick = {
                        BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
                    })
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            MyTabStrip(
                tabs, JewelTheme.editorTabStyle,
            )
            val selectApp = flutterAppList.getOrNull(tabIndex)
            if (selectApp != null) {
                body.invoke(selectApp)
            }
        }
    }
}

@Composable
fun MyTabStrip(tabs: List<TabData>, style: TabStyle, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TabStrip(tabs, style, modifier)
}