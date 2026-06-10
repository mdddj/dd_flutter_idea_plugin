package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import icons.MyImages
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DropdownLink
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.api.vm.DartVmApp
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.common.dart.FlutterXVMService
import shop.itbug.flutterx.config.PluginConfig
import shop.itbug.flutterx.constance.discordUrl
import shop.itbug.flutterx.constance.qqGroup
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.widget.CenterText
import shop.itbug.flutterx.widget.CustomTabRow
import shop.itbug.flutterx.widget.KofiWidget
import java.net.URI

private const val DART_VM_PANEL_DOC_SERVER_URL = "https://flutterx.itbug.shop/en/vm/dart_vm_panel/"
private const val DART_VM_PANEL_DOC_GITHUB_URL = "https://mdddj.github.io/flutterx-doc/en/vm/dart_vm_panel/"
private const val DART_VM_DEV_TOOL_EXTENSION_DOC_SERVER_URL =
    "https://flutterx.itbug.shop/en/vm/dart_vm_devtool_extension/"
private const val DART_VM_DEV_TOOL_EXTENSION_DOC_GITHUB_URL =
    "https://mdddj.github.io/flutterx-doc/en/vm/dart_vm_devtool_extension/"

@Composable
fun FlutterAppsTabComponent(context: DartVmDevToolContext, body: @Composable (app: DartVmApp) -> Unit) {
    val flutterAppList by context.runningApps.collectAsState()
    FlutterAppsTabContent(
        project = context.project,
        flutterAppList = flutterAppList,
        appTitle = DartVmApp::deviceId,
        body = body
    )
}

@Composable
fun FlutterAppsTabComponent(project: Project, body: @Composable (app: FlutterAppInstance) -> Unit) {
    val flutterAppList by FlutterXVMService.getInstance(project).runningApps.collectAsState()
    FlutterAppsTabContent(
        project = project,
        flutterAppList = flutterAppList,
        appTitle = { it.appInfo.deviceId },
        body = body
    )
}

@Composable
private fun <T> FlutterAppsTabContent(
    project: Project,
    flutterAppList: List<T>,
    appTitle: (T) -> String,
    body: @Composable (app: T) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    var rewardPopup by remember { mutableStateOf<RewardQrCode?>(null) }
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    EmptyStateLinkGroup(label = PluginBundle.get("doc")) {
                        DocumentationDropdownLink(
                            text = PluginBundle.get("vm.flutterapps.panel.doc"),
                            icon = AllIconsKeys.Gutter.Web,
                            serverUrl = DART_VM_PANEL_DOC_SERVER_URL,
                            githubUrl = DART_VM_PANEL_DOC_GITHUB_URL
                        )
                        DocumentationDropdownLink(
                            text = PluginBundle.get("vm.flutterapps.extension.doc"),
                            icon = AllIconsKeys.Plugins.PluginLogo,
                            serverUrl = DART_VM_DEV_TOOL_EXTENSION_DOC_SERVER_URL,
                            githubUrl = DART_VM_DEV_TOOL_EXTENSION_DOC_GITHUB_URL
                        )
                    }

                    EmptyStateLinkGroup(label = PluginBundle.get("vm.flutterapps.group.other")) {
                        val pluginConfig = PluginConfig.getState(project)
                        if (pluginConfig.showDiscord) {
                            IconLink(
                                text = "Discord",
                                icon = MyIcons.discord,
                                onClick = {
                                    BrowserUtil.browse(discordUrl)
                                }
                            )
                        }
                        if (pluginConfig.showQQGroup) {
                            IconLink(
                                text = "QQ Group",
                                icon = MyIcons.qq,
                                onClick = {
                                    BrowserUtil.browse(qqGroup)
                                }
                            )
                        }

                        IconLink(
                            text = PluginBundle.get("bugs"),
                            icon = AllIconsKeys.Ide.Feedback,
                            onClick = {
                                BrowserUtil.open("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
                            }
                        )
                    }

                    if (showRewardAction) {
                        EmptyStateLinkGroup(label = PluginBundle.get("reward")) {
                            RewardQrCodeLink(
                                text = PluginBundle.get("vm.flutterapps.reward.wechat"),
                                linkIcon = MyIcons.wechat,
                                qrCode = RewardQrCode(
                                    icon = MyImages.wxDs,
                                    contentDescription = PluginBundle.get("vm.flutterapps.reward.wechat.desc")
                                ),
                                popup = rewardPopup,
                                onPopupChange = { rewardPopup = it }
                            )
                            RewardQrCodeLink(
                                text = PluginBundle.get("vm.flutterapps.reward.alipay"),
                                linkIcon = MyIcons.alipay,
                                qrCode = RewardQrCode(
                                    icon = MyImages.alipayDs,
                                    contentDescription = PluginBundle.get("vm.flutterapps.reward.alipay.desc")
                                ),
                                popup = rewardPopup,
                                onPopupChange = { rewardPopup = it }
                            )
                            KofiWidget()
                        }
                    }
                }

            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomTabRow(
                tabIndex,
                tabs = flutterAppList.map(appTitle),
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

private data class RewardQrCode(
    val icon: IconKey,
    val contentDescription: String
)

@Composable
private fun RewardQrCodeLink(
    text: String,
    linkIcon: IconKey,
    qrCode: RewardQrCode,
    popup: RewardQrCode?,
    onPopupChange: (RewardQrCode?) -> Unit
) {
    Box {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                linkIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Link(
                text,
                modifier = Modifier.focusProperties { canFocus = false },
                onClick = {
                    onPopupChange(if (popup == qrCode) null else qrCode)
                }
            )
        }

        if (popup == qrCode) {
            Popup(onDismissRequest = { onPopupChange(null) }) {
                Box(
                    modifier = Modifier.background(JewelTheme.globalColors.panelBackground)
                        .border(1.dp, JewelTheme.globalColors.borders.normal)
                        .padding(12.dp)
                ) {
                    Icon(
                        qrCode.icon,
                        modifier = Modifier.size(200.dp),
                        contentDescription = qrCode.contentDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateLinkGroup(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label:", color = JewelTheme.globalColors.text.info)
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun DocumentationDropdownLink(
    text: String,
    icon: IconKey,
    serverUrl: String,
    githubUrl: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        DropdownLink(
            text = text,
            modifier = Modifier.focusProperties { canFocus = false }
        ) {
            selectableItem(
                selected = false,
                onClick = {
                    BrowserUtil.browse(URI.create(serverUrl))
                },
            ) {
                Text(PluginBundle.get("vm.flutterapps.extension.doc.server"))
            }
            separator()
            selectableItem(
                selected = false,
                onClick = {
                    BrowserUtil.browse(URI.create(githubUrl))
                },
            ) {
                Text(PluginBundle.get("vm.flutterapps.extension.doc.github"))
            }
        }
    }
}

@Composable
private fun IconLink(
    text: String,
    icon: IconKey,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Link(
            text,
            modifier = Modifier.focusProperties { canFocus = false },
            onClick = onClick
        )
    }
}
