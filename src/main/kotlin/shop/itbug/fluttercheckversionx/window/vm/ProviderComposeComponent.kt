package shop.itbug.fluttercheckversionx.window.vm

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.fluttercheckversionx.widget.CenterText
import vm.VmService
import vm.devtool.*


/**
 * flutter provider component for toolwindow
 */
@Composable
fun ProviderComposeComponent(project: Project) {
    FlutterAppsTabComponent(project) {
        CenterText("Coming soon...")
//        ProviderBody(project, it.vmService)
    }
}


@Composable
private fun ProviderBody(project: Project, vmService: VmService) {
    var selectProvider by remember { mutableStateOf<ProviderNode?>(null) }
    var outerSplitState by mutableStateOf(SplitLayoutState(0.5f))
    HorizontalSplitLayout(
        state = outerSplitState,
        first = {
            ProviderList(project, vmService) {
                selectProvider = it
            }
        },
        second = {
            if (selectProvider != null) {
                ProviderDetails(vmService, selectProvider!!)
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Select a provider to see details", color = JewelTheme.globalColors.text.info)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        firstPaneMinWidth = 100.dp,
        secondPaneMinWidth = 100.dp,
    )
}

//列表
@Composable
private fun ProviderList(project: Project, vm: VmService, onSelectChange: (item: ProviderNode) -> Unit) {
    val scope = vm.coroutineScope
    var providers by remember { mutableStateOf<List<ProviderNode>>(emptyList()) }
    fun refresh() {
        scope.launch {
            providers = ProviderHelper.getProviderNodes(vm)
        }
    }

    LaunchedEffect(vm) {
        refresh()
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row {
            IconActionButton(AllIconsKeys.Actions.Refresh, contentDescription = "Refresh", onClick = {
                refresh()
            })
        }
        for (node in providers)
            Box(modifier = Modifier.clickable(onClick = {
                onSelectChange.invoke(node)
            })) {
                Text(node.type)
            }
    }
}


/**
 * Provider 详情展示面板
 */
@Composable
private fun ProviderDetails(vmService: VmService, provider: ProviderNode) {
    val rootPath = remember(provider.id) { InstancePath.FromProviderId(provider.id) }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
        InstanceNodeViewer(vmService = vmService, path = rootPath)
    }
}

/**
 * 递归的 Composable，用于显示一个实例节点及其子节点。
 */
@Composable
private fun InstanceNodeViewer(vmService: VmService, path: InstancePath) {
    var details by remember(path) { mutableStateOf<InstanceDetails?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var isExpanded by remember(path) { mutableStateOf(path.pathToProperty.isEmpty()) } // 根节点默认展开

    LaunchedEffect(path) {
        try {
            details = ProviderHelper.getInstanceDetails(vmService, path)
        } catch (e: Exception) {
            error = e.message ?: "An unknown error occurred"
        }
    }

    if (error != null) {
        Text("Error: $error", color = JewelTheme.globalColors.text.error)
        return
    }

    if (details == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Loading...")
        }
        return
    }

    val currentDetails = details!!

    Column {
        InstanceHeader(
            details = currentDetails,
            isExpanded = isExpanded,
            isExpandable = currentDetails.isExpandable,
            onToggleExpand = { isExpanded = !isExpanded }
        )

        if (isExpanded) {
            Box(modifier = Modifier.padding(start = 20.dp)) {
                when (currentDetails) {
                    is InstanceDetails.Object -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            currentDetails.fields.forEach { field ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("${field.name}: ", fontWeight = FontWeight.Bold)
                                    InstanceNodeViewer(
                                        vmService = vmService,
                                        path = path.pathForChildWithInstance(
                                            PathToProperty.ObjectProperty(
                                                name = field.name,
                                                ownerName = field.ownerName,
                                                ownerUri = field.ownerUri
                                            ),
                                            field.instanceId
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is InstanceDetails.DartList -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until currentDetails.length) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("[$i]: ")
                                    InstanceNodeViewer(
                                        vmService = vmService,
                                        path = path.pathForChild(PathToProperty.ListIndex(i))
                                    )
                                }
                            }
                        }
                    }

                    is InstanceDetails.Map -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            currentDetails.associations.forEach { assoc ->
                                Row(verticalAlignment = Alignment.Top) {
                                    // Key 部分 (也需要一个新的根路径)
                                    val keyId = assoc.getKey()?.getId()
                                    if (keyId != null) {
                                        InstanceNodeViewer(
                                            vmService = vmService,
                                            path = InstancePath.FromInstanceId(keyId)
                                        )
                                    } else {
                                        Text("null")
                                    }
                                    Text(": ")
                                    // Value 部分
                                    val valueId = assoc.getValue()?.getId()
                                    if (valueId != null) {
                                        InstanceNodeViewer(
                                            vmService = vmService,
                                            path = InstancePath.FromInstanceId(valueId)
                                        )
                                    } else {
                                        Text("null")
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

/**
 * 显示实例头部信息，包括类型、值和展开按钮。
 */
@Composable
private fun InstanceHeader(
    details: InstanceDetails,
    isExpanded: Boolean,
    isExpandable: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = isExpandable, onClick = onToggleExpand)
    ) {
        if (isExpandable) {
            val iconKey = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight
            Icon(key = iconKey, contentDescription = "Expand/Collapse")
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(20.dp))
        }

        when (details) {
            is InstanceDetails.DartString -> Text("\"${details.displayString}\"", color = Color(0xFFCE9178))
            is InstanceDetails.Number -> Text(details.displayString, color = Color(0xFFB5CEA8))
            is InstanceDetails.Bool -> Text(details.displayString, color = Color(0xFF569CD6))
            is InstanceDetails.Nill -> Text("null", color = Color(0xFF569CD6))
            is InstanceDetails.Object -> Text(
                "${details.type} #${details.hash.toString(16).take(4)}",
                color = Color(0xFF4EC9B0)
            )

            is InstanceDetails.DartList -> Text(
                "List (${details.length} elements) #${
                    details.hash.toString(16).take(4)
                }"
            )

            is InstanceDetails.Map -> Text(
                "Map (${details.associations.size} entries) #${
                    details.hash.toString(16).take(4)
                }"
            )

            is InstanceDetails.Enum -> Text("${details.type}.${details.value}")
        }
    }
}
