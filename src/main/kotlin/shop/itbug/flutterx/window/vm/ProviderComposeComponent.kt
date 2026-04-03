package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.services.PubspecService
import shop.itbug.flutterx.util.firstChatToUpper
import vm.VmService
import vm.devtool.*

/** flutter provider component for toolwindow */
@Composable
fun ProviderComposeComponent(project: Project) {
    val pubspecService = PubspecService.getInstance(project)
    val dependenciesNames by pubspecService.dependenciesNamesFlow.collectAsState()
    val isUseProviderDeps = dependenciesNames.contains("provider")
    FlutterAppsTabComponent(project) {
        if (!isUseProviderDeps) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("notfound_provider_deps"))
            }
        } else {
            ProviderBody(project, it.vmService)
        }
    }
}

@Composable
private fun ProviderBody(project: Project, vmService: VmService) {
    val pubspecService = PubspecService.getInstance(project)

    // 使用状态管理类
    val providerState = remember(vmService) { ProviderState(vmService) }
    LaunchedEffect(vmService) {
        providerState.refreshProviders()
    }

    val details by pubspecService.detailsFlow.collectAsState()
    val providerInfo = details.find { it.name == "provider" }

    HorizontalSplitLayout(
        state = providerState.splitState,
        first = {
            ProviderList(providerState)
        },
        second = {
            val select = providerState.selectedProvider
            if (select != null) {
                ProviderDetails(vmService, select)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        key = AllIconsKeys.General.Beta,
                        contentDescription = PluginBundle.get("provider.beta.content.desc")
                    )
                    Text(
                        PluginBundle.get("provider.select.details"),
                        color = JewelTheme.globalColors.text.info
                    )
                    if (providerInfo != null)
                        Text(
                            PluginBundle.get("provider.version.label", providerInfo.version),
                            color = JewelTheme.globalColors.text.info
                        )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        firstPaneMinWidth = 100.dp,
        secondPaneMinWidth = 100.dp,
    )
}

@Composable
private fun ProviderList(state: ProviderState) {
    val searchState = rememberTextFieldState("")
    val query = searchState.text.toString().trim()
    val filteredProviders = remember(state.providers, query) {
        if (query.isBlank()) {
            state.providers
        } else {
            val q = query.lowercase()
            state.providers.filter { node ->
                node.type.lowercase().contains(q) || node.id.lowercase().contains(q)
            }
        }
    }
    val listState = rememberLazyListState()
    val selectedId = state.selectedProvider?.id

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                key = AllIconsKeys.General.Beta,
                contentDescription = PluginBundle.get("provider.beta.content.desc")
            )
            IconActionButton(
                AllIconsKeys.Actions.Refresh,
                contentDescription = PluginBundle.get("provider.refresh.content.desc"),
                onClick = {
                    if (!state.isLoading) {
                        state.refreshProviders()
                    }
                }
            )
            Text(
                "${filteredProviders.size}/${state.providers.size}",
                color = JewelTheme.globalColors.text.info
            )
        }

        TextField(
            state = searchState,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(PluginBundle.get("provider.filter.placeholder")) },
        )

        if (state.isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(PluginBundle.get("provider.loading"))
            }
        }

        if (state.error != null) {
            Text(
                PluginBundle.get("provider.error", state.error.orEmpty()),
                color = JewelTheme.globalColors.text.error
            )
        }

        if (filteredProviders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("provider.empty"), color = JewelTheme.globalColors.text.info)
            }
            return@Column
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredProviders, key = { it.id }) { node ->
                    val isSelected = selectedId == node.id
                    val bgColor = if (isSelected) {
                        JewelTheme.globalColors.panelBackground
                    } else if (JewelTheme.isDark) {
                        Color(0xFF2D2D30)
                    } else {
                        Color.White
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) JewelTheme.globalColors.borders.focused else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable(onClick = { state.selectProvider(node) })
                            .pointerHoverIcon(PointerIcon.Hand)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(node.type, maxLines = 1)
                            Text(
                                text = node.id,
                                color = JewelTheme.globalColors.text.info,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
    }
}

/** Provider 详情展示面板 */
@Composable
private fun ProviderDetails(vmService: VmService, provider: ProviderNode) {
    val rootPath = remember(provider, provider.id) { provider.getProviderPath() }
    Column(modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(rememberScrollState())) {
        InstanceNodeViewer(vmService = vmService, path = rootPath)
    }
}

/** 递归的 Composable，用于显示一个实例节点及其子节点。 */
@Composable
private fun InstanceNodeViewer(
    vmService: VmService,
    path: InstancePath,
    parent: InstanceDetails? = null,
    parentInstanceId: String? = null,
    field: ObjectField? = null,
    expandedByDefault: Boolean = path is InstancePath.FromProviderId && parent == null,
) {
    var details by remember(path) { mutableStateOf<InstanceDetails?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var isExpanded by remember(path, expandedByDefault) { mutableStateOf(expandedByDefault) } // 仅 provider 根节点默认展开
    var refreshTrigger by remember { mutableStateOf(0) } // 刷新触发器

    LaunchedEffect(path, path.pathToProperty, refreshTrigger) {
        try {
            details = ProviderHelper.getInstanceDetails(vmService, path, parent)
            error = null
        } catch (e: Exception) {
            error = e.message ?: PluginBundle.get("provider.unknown.error")
        }
    }

    if (error != null) {
        Text(PluginBundle.get("provider.error", error.orEmpty()), color = JewelTheme.globalColors.text.error)
        return
    }

    if (details == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(PluginBundle.get("provider.loading"))
        }
        return
    }

    val currentDetails = details!!

    Column {
        InstanceHeader(
            details = currentDetails,
            isExpanded = isExpanded,
            isExpandable = currentDetails.isExpandable,
            onToggleExpand = { isExpanded = !isExpanded },
            vmService = vmService,
            parentInstanceId = parentInstanceId,
            field = field,
            onValueUpdated = {
                // 触发刷新
                refreshTrigger++
            }
        )

        if (isExpanded) {
            Box(modifier = Modifier.padding(start = 20.dp)) {
                when (currentDetails) {
                    is InstanceDetails.Object -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            currentDetails
                                .fields
//                                    .filter { it.isDefinedByDependency.not() }
//                                    .filter { it.isStatic.not() }
                                .forEach { field ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text("${field.name}: ", fontWeight = FontWeight.Bold)

                                        InstanceNodeViewer(
                                            vmService = vmService,
                                            path =
                                                path.pathForChildWithInstance(
                                                    PathToProperty.ObjectProperty(
                                                        name = field.name,
                                                        ownerName =
                                                            field.ownerName,
                                                        ownerUri =
                                                            field.ownerUri,
                                                        field = field
                                                    ),
                                                    field.ref.getId()
                                                ),
                                            parent = currentDetails,
                                            parentInstanceId = currentDetails.instanceRefId,
                                            field = field
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
                                        path = path.pathForChild(PathToProperty.ListIndex(i)),
                                        parent = currentDetails
                                    )
                                }
                            }
                        }
                    }

                    is InstanceDetails.Map -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            currentDetails.associations.forEach { assoc ->
                                Row(verticalAlignment = Alignment.Top) {
                                    // Key 部分 - 直接显示，不需要展开
                                    val keyRef = assoc.getKey()
                                    if (keyRef != null) {
                                        InstanceNodeViewer(
                                            vmService = vmService,
                                            path = InstancePath.FromInstanceId(keyRef.getId()),
                                            expandedByDefault = false,
                                        )
                                    } else {
                                        Text(PluginBundle.get("provider.value.null"))
                                    }
                                    Text(": ")
                                    // Value 部分 - 通过 MapKey 路径获取
                                    val keyId = keyRef?.getId()
                                    InstanceNodeViewer(
                                        vmService = vmService,
                                        path = path.pathForChild(PathToProperty.MapKey(keyId)),
                                        parent = currentDetails
                                    )
                                }
                            }
                        }
                    }

                    is InstanceDetails.Enum -> {
                        Text("${currentDetails.type}.${currentDetails.value}")
                    }

                    else -> {}
                }
            }
        }
    }
}

/** 显示实例头部信息，包括类型、值和展开按钮。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstanceHeader(
    details: InstanceDetails,
    isExpanded: Boolean,
    isExpandable: Boolean,
    onToggleExpand: () -> Unit,
    vmService: VmService? = null,
    parentInstanceId: String? = null,
    field: ObjectField? = null,
    onValueUpdated: (() -> Unit)? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 判断是否可编辑
    val isEditable = field != null && !field.isFinal && parentInstanceId != null && vmService != null &&
            (details is InstanceDetails.DartString || details is InstanceDetails.Number ||
                    details is InstanceDetails.Bool || details is InstanceDetails.Enum)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(enabled = isExpandable, onClick = onToggleExpand)
    ) {
        if (isExpandable) {
            val iconKey =
                if (isExpanded) AllIconsKeys.General.ArrowDown
                else AllIconsKeys.General.ArrowRight
            Icon(key = iconKey, contentDescription = PluginBundle.get("provider.expand.collapse.content.desc"))
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(20.dp))
        }

        if (isEditing && isEditable) {
            // 编辑模式
            EditValueField(
                details = details,
                onConfirm = { newValue ->
                    scope.launch {
                        val success = ProviderHelper.updateFieldValue(
                            vmService,
                            parentInstanceId,
                            field,
                            newValue,
                            triggerNotify = true
                        )
                        if (success) {
                            isEditing = false
                            // 延迟一下再刷新，确保 VM 已经更新
                            delay(300)
                            onValueUpdated?.invoke()
                        }
                    }
                },
                onCancel = { isEditing = false }
            )
        } else {
            // 显示模式
            when (details) {
                is InstanceDetails.DartString -> Text(details.displayString, color = Color(0xFFCE9178))
                is InstanceDetails.Number -> Text(details.displayString, color = Color(0xFFB5CEA8))
                is InstanceDetails.Bool -> Text(details.displayString, color = Color(0xFF569CD6))
                is InstanceDetails.Nill -> Text(PluginBundle.get("provider.value.null"), color = Color(0xFF569CD6))
                is InstanceDetails.Object ->
                    Text(
                        "${details.type} #${details.hash.toString(16).take(4)}",
                        color = Color(0xFF4EC9B0)
                    )

                is InstanceDetails.DartList ->
                    Text(
                        PluginBundle.get("provider.list.summary", details.length, details.hash.toString(16).take(4))
                    )

                is InstanceDetails.Map ->
                    Text(
                        PluginBundle.get(
                            "provider.map.summary",
                            details.associations.size,
                            details.hash.toString(16).take(4)
                        )
                    )

                is InstanceDetails.Enum -> Text("${details.type}.${details.value}")
            }

            // 编辑按钮
            if (isEditable) {
                Spacer(Modifier.width(8.dp))
                Text(
                    PluginBundle.get("edit").firstChatToUpper(),
                    color = JewelTheme.globalColors.text.info,
                    modifier = Modifier.clickable { isEditing = true }.pointerHoverIcon(PointerIcon.Hand)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditValueField(
    details: InstanceDetails,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initialValue = when (details) {
        is InstanceDetails.DartString -> "\"${details.displayString}\""
        is InstanceDetails.Number -> details.displayString
        is InstanceDetails.Bool -> details.displayString
        is InstanceDetails.Enum -> "${details.type}.${details.value}"
        else -> ""
    }

    val textState = rememberTextFieldState(initialValue)

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextField(
            state = textState,
            modifier = Modifier.width(200.dp)
        )
        OutlinedButton(
            onClick = { onConfirm(textState.text.toString()) }
        ) {
            Text(PluginBundle.get("submit").firstChatToUpper())
        }
        OutlinedButton(
            onClick = onCancel
        ) {
            Text(PluginBundle.get("cancel").firstChatToUpper())
        }
    }
}
