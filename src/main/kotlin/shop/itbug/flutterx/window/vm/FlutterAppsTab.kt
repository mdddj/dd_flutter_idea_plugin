package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import icons.MyImages
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.common.dart.FlutterXVMService
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.constance.discordUrl
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.widget.CenterText
import shop.itbug.flutterx.widget.CustomTabRow
import shop.itbug.flutterx.widget.KofiWidget
import java.net.URI


@Composable
fun FlutterAppsTabComponent(project: Project, body: @Composable (app: FlutterAppInstance) -> Unit) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var showRewardPopup by remember { mutableStateOf(false) }
    val flutterAppList by FlutterXVMService.getInstance(project).runningApps.collectAsState()
    val isEnableFuture = FlutterXVMService.getInstance(project).isEnableFuture.collectAsState().value
    val showRewardAction = PluginConfig.getState(project).showRewardAction
    if (flutterAppList.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                if (isEnableFuture.not()) {
                    Text(PluginBundle.get("vm.flutterapps.feature.disabled"), color = JewelTheme.globalColors.text.error)
                }
                CenterText(
                    PluginBundle.get("vm.flutterapps.notfound.message")
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Link(PluginBundle.get("doc"), onClick = {
                        BrowserUtil.browse(URI.create("https://flutterx.itbug.shop/en/vm/dart_vm_panel/"))
                    })
                    if (PluginConfig.getState(project).showDiscord) {
                        Link("Discord", onClick = {
                            BrowserUtil.browse(discordUrl)
                        })
                    }

                    Link(PluginBundle.get("bugs"), onClick = {
                        BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
                    })

                    if (showRewardAction) {
                        Box {
                            Link(PluginBundle.get("vm.flutterapps.reward.wechat"), onClick = {
                                showRewardPopup = !showRewardPopup
                            })

                            if (showRewardPopup) {
                                Popup(onDismissRequest = { showRewardPopup = false }) {
                                    Box(
                                        modifier = Modifier.background(JewelTheme.globalColors.panelBackground)
                                            .border(1.dp, JewelTheme.globalColors.borders.normal)
                                            .padding(12.dp)
                                    ) {
                                        Icon(
                                            MyImages.wxDs,
                                            modifier = Modifier.size(200.dp),
                                            contentDescription = PluginBundle.get("vm.flutterapps.reward.wechat.desc")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showRewardAction) {
                        KofiWidget()
                    }
                }

            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomTabRow(
                tabIndex,
                tabs = flutterAppList.map { it.appInfo.deviceId },
                onTabClick = {
                    tabIndex = it
                },
                modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground)
            )
            val selectApp = flutterAppList.getOrNull(tabIndex)
            if (selectApp != null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    body.invoke(selectApp)
                }
            }
        }
    }
}
