package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.actions.context.SiteDocument
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.i18n.PluginBundle
import vm.hive.HiveBoxState
import vm.hive.HiveConnectAction
import vm.hive.HiveExtensionStatus
import vm.hive.HiveInspectorState
import vm.hive.HiveListRef
import vm.hive.HiveRawObject
import vm.hive.HiveServices
import vm.hive.HiveTableEntry
import vm.hive.isAnonymousHiveObject
import vm.hive.needsHiveRuntimeResolution
import vm.hive.supportsHiveRuntimeLookupKey
import vm.hive.toHiveAnonymousFieldIndexOrNull
import vm.hive.toHiveChildEntries
import vm.hive.toHiveSearchableString
import vm.hive.toHiveSummaryText

@Composable
fun DartVmHiveComponent(project: Project) {
    FlutterAppsTabComponent(project) { app ->
        HiveMainPanel(project, app)
    }
}

@Composable
private fun HiveMainPanel(project: Project, app: FlutterAppInstance) {
    val service = remember(app.vmService) { HiveServices(project, app.vmService) }
    val state by service.state.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedBox by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(service) {
        service.initialize()
    }

    DisposableEffect(service) {
        onDispose { service.dispose() }
    }

    val boxNames = state.boxes.keys
    LaunchedEffect(boxNames) {
        if (selectedBox !in boxNames) {
            selectedBox = state.boxes.values.firstOrNull { it.open }?.name
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalSplitLayout(
            state = remember { SplitLayoutState(0.24f) },
            modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, JewelTheme.globalColors.borders.normal),
            firstPaneMinWidth = 180.dp,
            secondPaneMinWidth = 320.dp,
            first = {
                HiveBoxesPanel(
                    state = state,
                    selectedBox = selectedBox,
                    onSelect = { boxName ->
                        selectedBox = boxName
                        scope.launch {
                            service.loadBox(boxName)
                        }
                    },
                    onRefresh = { scope.launch { service.refreshBoxes() } },
                )
            },
            second = {
                HiveDetailsPanel(
                    state = state,
                    selectedBox = selectedBox,
                    onLoadBox = { boxName ->
                        scope.launch { service.loadBox(boxName) }
                    },
                    onLoadValue = { boxName, key ->
                        scope.launch { service.loadLazyValue(boxName, key) }
                    },
                    onResolveFields = { boxName, key ->
                        scope.launch { service.resolveRuntimeValue(boxName, key) }
                    },
                )
            },
        )

        HiveStatusBar(state)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiveBoxesPanel(
    state: HiveInspectorState,
    selectedBox: String?,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val filteredBoxes = remember(state.boxes, query) {
        state.boxes.values
            .sortedBy { it.name.lowercase() }
            .filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(PluginBundle.get("vm.hive.boxes.title"), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconActionButton(
                    key = AllIconsKeys.Actions.Help,
                    contentDescription = PluginBundle.get("doc"),
                    onClick = { BrowserUtil.browse(SiteDocument.Hive.url) },
                ) {
                    Text(PluginBundle.get("doc"))
                }
                IconActionButton(
                    key = AllIconsKeys.Actions.Refresh,
                    contentDescription = PluginBundle.get("vm.hive.refresh"),
                    onClick = onRefresh,
                ) {
                    Text(PluginBundle.get("vm.hive.refresh"))
                }
            }
        }

        Divider(orientation = Orientation.Horizontal)

        TextField(
            state = searchState,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text(PluginBundle.get("vm.hive.search.boxes")) },
        )

        Divider(orientation = Orientation.Horizontal)

        when {
            state.status == HiveExtensionStatus.Unavailable -> {
                HiveUnavailablePanel(modifier = Modifier.fillMaxSize())
            }

            state.refreshing && state.boxes.isEmpty() -> {
                HiveLoadingPanel(modifier = Modifier.fillMaxSize(), text = PluginBundle.get("vm.hive.status.loading"))
            }

            filteredBoxes.isEmpty() -> {
                HiveEmptyPanel(
                    modifier = Modifier.fillMaxSize(),
                    text = if (state.boxes.isEmpty()) PluginBundle.get("vm.hive.empty.boxes")
                    else PluginBundle.get("vm.hive.empty.search"),
                )
            }

            else -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filteredBoxes, key = { it.name }) { box ->
                        HiveBoxListItem(
                            box = box,
                            isSelected = box.name == selectedBox,
                            onClick = { onSelect(box.name) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiveBoxListItem(
    box: HiveBoxState,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) {
        JewelTheme.globalColors.outlines.focused.copy(alpha = 0.12f)
    } else {
        JewelTheme.globalColors.panelBackground
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                box.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (!box.open) {
                Text(
                    PluginBundle.get("vm.hive.box.closed"),
                    color = JewelTheme.globalColors.text.info,
                    modifier = Modifier.alpha(0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        val subtitle = if (box.loaded) {
            PluginBundle.get("vm.hive.box.summary.loaded", box.frames.size)
        } else {
            PluginBundle.get("vm.hive.box.summary.not.loaded")
        }
        Text(
            subtitle,
            color = JewelTheme.globalColors.text.info,
            modifier = Modifier.alpha(0.8f),
        )
    }
}

@Composable
private fun HiveDetailsPanel(
    state: HiveInspectorState,
    selectedBox: String?,
    onLoadBox: (String) -> Unit,
    onLoadValue: (String, Any) -> Unit,
    onResolveFields: (String, Any) -> Unit,
) {
    if (selectedBox == null) {
        HiveEmptyPanel(
            modifier = Modifier.fillMaxSize(),
            text = PluginBundle.get("vm.hive.select.box"),
        )
        return
    }

    val box = state.boxes[selectedBox]
    if (box == null) {
        HiveLoadingPanel(
            modifier = Modifier.fillMaxSize(),
            text = PluginBundle.get("vm.hive.status.loading.box", selectedBox),
        )
        return
    }

    LaunchedEffect(box.name, box.loaded) {
        if (!box.loaded && state.status != HiveExtensionStatus.Unavailable) {
            onLoadBox(box.name)
        }
    }

    if (!box.loaded) {
        HiveLoadingPanel(
            modifier = Modifier.fillMaxSize(),
            text = PluginBundle.get("vm.hive.status.loading.box", box.name),
        )
        return
    }

    if (box.frames.isEmpty()) {
        HiveEmptyPanel(
            modifier = Modifier.fillMaxSize(),
            text = PluginBundle.get("vm.hive.empty.frames"),
        )
        return
    }

    key(box.name) {
        HiveBoxNavigator(
            box = box,
            onLoadValue = { key -> onLoadValue(box.name, key) },
            onResolveFields = { key -> onResolveFields(box.name, key) },
        )
    }
}

private data class HiveStackItem(
    val label: String,
    val entries: List<HiveTableEntry>,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiveBoxNavigator(
    box: HiveBoxState,
    onLoadValue: (Any) -> Unit,
    onResolveFields: (Any) -> Unit,
) {
    var stack by remember { mutableStateOf<List<HiveStackItem>>(emptyList()) }
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val breadcrumbScrollState = rememberScrollState()

    val rootEntries = remember(box.frames) {
        box.frames.values
            .toList()
            .asReversed()
            .map { HiveTableEntry(it.key, it.value, it.lazy) }
    }
    val visibleEntries = remember(rootEntries, stack, query) {
        val currentEntries = stack.lastOrNull()?.entries ?: rootEntries
        if (query.isBlank()) {
            currentEntries
        } else {
            currentEntries.filter { entry ->
                entry.key.toString().contains(query, ignoreCase = true) ||
                        entry.value.toHiveSearchableString().contains(query, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(breadcrumbScrollState),
            ) {
                HiveBreadcrumbs(
                    root = box.name,
                    stack = stack,
                    onNavigate = { index ->
                        stack = if (index < 0) emptyList() else stack.take(index + 1)
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    PluginBundle.get("vm.hive.box.summary.loaded", box.frames.size),
                    color = JewelTheme.globalColors.text.info,
                    modifier = Modifier.alpha(0.8f),
                )
            }
        }

        Divider(orientation = Orientation.Horizontal)

        TextField(
            state = searchState,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text(PluginBundle.get("vm.hive.search.values")) },
        )

        Divider(orientation = Orientation.Horizontal)

        if (visibleEntries.isEmpty()) {
            HiveEmptyPanel(
                modifier = Modifier.fillMaxSize(),
                text = PluginBundle.get("vm.hive.empty.search"),
            )
            return@Column
        }

        HiveTable(
            entries = visibleEntries,
            onLoadValue = onLoadValue,
            onResolveFields = onResolveFields,
            onNavigate = { entry ->
                val childEntries = entry.value.toHiveChildEntries() ?: return@HiveTable
                stack = stack + HiveStackItem(hiveDisplayKeyText(entry.key), childEntries)
            },
        )
    }
}

private fun hiveDisplayKeyText(key: Any): String {
    val raw = key.toString()
    val anonymousIndex = raw.toHiveAnonymousFieldIndexOrNull()
    return if (anonymousIndex != null) {
        PluginBundle.get("vm.hive.field.index", anonymousIndex)
    } else {
        raw
    }
}

@Composable
private fun HiveBreadcrumbs(
    root: String,
    stack: List<HiveStackItem>,
    onNavigate: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Link(root, onClick = { onNavigate(-1) })
        stack.forEachIndexed { index, item ->
            Text(" / ", modifier = Modifier.alpha(0.7f))
            Link(item.label, onClick = { onNavigate(index) })
        }
    }
}

@Composable
private fun HiveTable(
    entries: List<HiveTableEntry>,
    onLoadValue: (Any) -> Unit,
    onResolveFields: (Any) -> Unit,
    onNavigate: (HiveTableEntry) -> Unit,
) {
    val horizontalScroll = rememberScrollState()
    val objectColumns = remember(entries) {
        if (entries.isNotEmpty() && entries.all { !it.lazy && it.value is HiveRawObject && !it.value.needsHiveRuntimeResolution() }) {
            entries
                .map { it.value as HiveRawObject }
                .flatMap { value -> value.fields.map { it.name } }
                .distinct()
        } else {
            emptyList()
        }
    }

    val cellWidth = 220.dp
    val valueColumnWidth = if (objectColumns.isEmpty()) 420.dp else cellWidth
    val totalColumns = 1 + maxOf(1, objectColumns.size)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScroll),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .width(cellWidth * totalColumns + if (objectColumns.isEmpty()) (valueColumnWidth - cellWidth) else 0.dp)
                    .background(JewelTheme.globalColors.panelBackground)
                    .border(1.dp, JewelTheme.globalColors.borders.normal),
            ) {
                HiveHeaderCell(
                    text = PluginBundle.get("vm.hive.table.key"),
                    width = cellWidth,
                )
                if (objectColumns.isEmpty()) {
                    HiveHeaderCell(
                        text = PluginBundle.get("vm.hive.table.value"),
                        width = valueColumnWidth,
                    )
                } else {
                    objectColumns.forEach { column ->
                        HiveHeaderCell(text = column, width = cellWidth)
                    }
                }
            }
        }

        items(entries, key = { it.key.toString() + if (it.lazy) ":lazy" else "" }) { entry ->
            Row(
                modifier = Modifier
                    .width(cellWidth * totalColumns + if (objectColumns.isEmpty()) (valueColumnWidth - cellWidth) else 0.dp)
                    .border(1.dp, JewelTheme.globalColors.borders.normal),
            ) {
                HiveKeyCell(entry.key.toString(), width = cellWidth)
                if (objectColumns.isEmpty()) {
                    HiveValueCell(
                        entry = entry,
                        width = valueColumnWidth,
                        onLoadValue = { onLoadValue(entry.key) },
                        onResolveFields = { onResolveFields(entry.key) },
                        onNavigate = { onNavigate(entry) },
                    )
                } else {
                    val rawObject = entry.value as? HiveRawObject
                    objectColumns.forEach { column ->
                        val fieldValue = rawObject?.fields?.firstOrNull { it.name == column }?.value
                        HiveValueCell(
                            entry = HiveTableEntry(column, fieldValue, lazy = false),
                            width = cellWidth,
                            onLoadValue = {},
                            onResolveFields = {},
                            onNavigate = {
                                onNavigate(HiveTableEntry(column, fieldValue, lazy = false))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiveHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HiveKeyCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
) {
    SelectionContainer {
        Box(
            modifier = Modifier
                .width(width)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                hiveDisplayKeyText(text),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HiveValueCell(
    entry: HiveTableEntry,
    width: androidx.compose.ui.unit.Dp,
    onLoadValue: () -> Unit,
    onResolveFields: () -> Unit,
    onNavigate: () -> Unit,
) {
    val childEntries = entry.value.toHiveChildEntries()
    val isNavigable = childEntries != null && childEntries.isNotEmpty()
    val canResolveFields = !entry.lazy && entry.key.supportsHiveRuntimeLookupKey() && entry.value.needsHiveRuntimeResolution()
    val isAnonymousObject = entry.value is HiveRawObject && entry.value.isAnonymousHiveObject()

    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            entry.lazy -> {
                OutlinedButton(onClick = onLoadValue) {
                    Text(PluginBundle.get("vm.hive.load.value"))
                }
            }

            isAnonymousObject -> {
                val value = entry.value as HiveRawObject
                HiveUnknownObjectCard(
                    value = value,
                    canResolveFields = canResolveFields,
                    onResolveFields = onResolveFields,
                    onNavigate = onNavigate,
                )
            }

            isNavigable -> {
                val label = when (entry.value) {
                    is HiveListRef -> PluginBundle.get("vm.hive.value.hivelist", entry.value.keys.size)
                    else -> entry.value.toHiveSummaryText()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, JewelTheme.globalColors.borders.normal, RoundedCornerShape(6.dp))
                            .clickable(onClick = onNavigate)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(label, color = JewelTheme.globalColors.text.info)
                    }
                    if (canResolveFields) {
                        OutlinedButton(onClick = onResolveFields) {
                            Text(PluginBundle.get("vm.hive.resolve.fields"))
                        }
                    }
                }
            }

            else -> {
                SelectionContainer {
                    Text(
                        entry.value.toHiveSummaryText(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HiveUnknownObjectCard(
    value: HiveRawObject,
    canResolveFields: Boolean,
    onResolveFields: () -> Unit,
    onNavigate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JewelTheme.globalColors.borders.normal, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (value.name.startsWith("Type#")) {
                    PluginBundle.get("vm.hive.unknown.object", value.name)
                } else {
                    value.name
                },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                PluginBundle.get("vm.hive.unknown.object.fields", value.fields.size),
                color = JewelTheme.globalColors.text.info,
                modifier = Modifier.alpha(0.8f),
            )
        }

        value.fields.take(3).forEach { field ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    hiveDisplayKeyText(field.name),
                    color = JewelTheme.globalColors.text.info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    field.value.toHiveSummaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (value.fields.size > 3) {
            Text(
                PluginBundle.get("vm.hive.unknown.object.more", value.fields.size - 3),
                color = JewelTheme.globalColors.text.info,
                modifier = Modifier.alpha(0.75f),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onNavigate) {
                Text(PluginBundle.get("vm.hive.open.details"))
            }
            if (canResolveFields) {
                OutlinedButton(onClick = onResolveFields) {
                    Text(PluginBundle.get("vm.hive.resolve.fields"))
                }
            }
        }
    }
}

@Composable
private fun HiveUnavailablePanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                key = AllIconsKeys.General.Warning,
                contentDescription = PluginBundle.get("vm.hive.unavailable.status"),
            )
            Text(
                PluginBundle.get("vm.hive.unavailable.message"),
                color = JewelTheme.globalColors.text.error,
            )
            Link(PluginBundle.get("doc"), onClick = { BrowserUtil.browse(SiteDocument.Hive.url) })
        }
    }
}

@Composable
private fun HiveLoadingPanel(
    modifier: Modifier = Modifier,
    text: String,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Text(text)
        }
    }
}

@Composable
private fun HiveEmptyPanel(
    modifier: Modifier = Modifier,
    text: String,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(text, color = JewelTheme.globalColors.text.info)
    }
}

@Composable
private fun HiveStatusBar(state: HiveInspectorState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JewelTheme.globalColors.borders.normal)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(state.statusMessage.ifBlank { PluginBundle.get("vm.hive.status.idle") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                PluginBundle.get("vm.hive.status.schemas", state.schemaFiles),
                color = JewelTheme.globalColors.text.info,
                modifier = Modifier.alpha(0.8f),
            )
            if (state.schemaFiles == 0) {
                Text(
                    PluginBundle.get("vm.hive.schemas.missing"),
                    color = JewelTheme.globalColors.text.info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(300.dp),
                )
            }
            state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    message,
                    color = JewelTheme.globalColors.text.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(360.dp),
                )
            }
        }
    }
}
