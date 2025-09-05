package shop.itbug.fluttercheckversionx.window.vm

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.fluttercheckversionx.actions.isValidJson
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import vm.log.LogData
import vm.log.LogLevel
import vm.log.LoggingController


// dart log UI
@OptIn(ExperimentalJewelApi::class)
@Composable
fun DartVmLoggingComponent(project: Project) {
    FlutterAppsTabComponent(project) {
        DartVmLoggingScreen(it.vmService.logController, project)
    }
}


@Composable
private fun DartVmLoggingScreen(controller: LoggingController, project: Project) {
    val logs by controller.filteredLogs.collectAsState()
    val status by controller.statusText.collectAsState()
    val selectedLog by controller.selectedLog.collectAsState()
    val searchQuery by controller.searchQuery.collectAsState()
    val hideGcLogs by controller.hideGcLogs.collectAsState()

    var splitState by remember { mutableStateOf(SplitLayoutState(0.5f)) }
    val listState = rememberLazyListState()

    LaunchedEffect(searchQuery) {
        controller.searchQuery.value = searchQuery
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, color = JewelTheme.globalColors.borders.normal)
            .background(JewelTheme.globalColors.panelBackground)
    ) {
        SearchBar(
            searchQuery = searchQuery,
            onQueryChange = { controller.searchQuery.value = it },
            hideGcLogs = hideGcLogs,
            onHideGcLogsToggle = { controller.hideGcLogs.value = it },
            onClearLogs = { controller.clear() }
        )

        HorizontalSplitLayout(
            state = splitState,
            modifier = Modifier.weight(1f),
            firstPaneMinWidth = 100.dp,
            secondPaneMinWidth = 100.dp,
            first = {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        items(
                            count = logs.size,
                            key = { "${logs[it].hashCode()}_$it" }
                        ) { index ->
                            val log = logs[index]
                            LogEntryRow(
                                log = log,
                                isSelected = log == selectedLog,
                                onClick = { controller.selectLog(log) }
                            )
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(
                            scrollState = listState
                        )
                    )
                }
            },
            second = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    if (selectedLog != null) {
                        LogDetailsPanel(log = selectedLog!!, project)
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Select a log to see details",
                                color = JewelTheme.globalColors.text.info
                            )
                        }
                    }
                }
            }
        )

        Divider(orientation = Orientation.Horizontal)

        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = status,
                color = JewelTheme.globalColors.text.info
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    hideGcLogs: Boolean,
    onHideGcLogsToggle: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchState = rememberTextFieldState(searchQuery)
    LaunchedEffect(searchState.text) {
        onQueryChange.invoke(searchState.text.toString())
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 清空日志按钮
        Tooltip(tooltip = { Text("Clear all logs") }) {
            IconButton(
                onClick = onClearLogs,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
            ) {
                Icon(
                    key = AllIconsKeys.General.Delete,
                    contentDescription = "Clear Logs"
                )
            }
        }

        Divider(orientation = Orientation.Vertical)

        // 搜索框
        TextField(
            state = searchState,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search logs (e.g., k:stdout, error)") },
        )

        Divider(orientation = Orientation.Vertical)

        // GC 日志过滤切换
        Tooltip(tooltip = { Text(if (hideGcLogs) "Show GC logs" else "Hide GC logs") }) {
            ToggleableIconButton(
                value = !hideGcLogs,
                onValueChange = { onHideGcLogsToggle(!it) },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Default)
            ) {
                Icon(
                    key = AllIconsKeys.Actions.ClearCash,
                    contentDescription = "Toggle GC Logs"
                )
            }
        }
    }
}


@Composable
private fun LogEntryRow(
    log: LogData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp, 2.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 时间戳
        Text(
            text = log.formattedTimestamp,
            modifier = Modifier.width(90.dp),
            color = JewelTheme.globalColors.text.info
        )

        LogLevelChip(log.level)

        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${log.kind}]",
                color = JewelTheme.globalColors.text.info,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = log.summary ?: log.details.value ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LogLevelChip(level: Int) {
    val textColor = when {
        level >= LogLevel.SEVERE -> JewelTheme.globalColors.text.error
        else -> JewelTheme.globalColors.text.normal
    }

    val text = when {
        level >= LogLevel.SEVERE -> "SEVERE"
        level >= LogLevel.INFO -> "INFO"
        else -> "LOG"
    }

    Text(
        text = text,
        color = textColor,
        fontWeight = if (level >= LogLevel.SEVERE) FontWeight.Bold else null,
        modifier = Modifier.width(60.dp)
    )
}


@Composable
private fun LogDetailsPanel(
    log: LogData,
    project: Project,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // 触发详情加载
    LaunchedEffect(log) {
        if (log.needsComputing) {
            scope.launch {
                log.computeDetails(this)
            }
        }
    }

    val details by log.details.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Log Details",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconActionButton(AllIconsKeys.Actions.Copy, onClick = {
                    details?.copyTextToClipboard()
                }, contentDescription = "Copy")
                OutlinedButton({
                    details?.let { MyFileUtil.showJsonInEditor(project, it) }
                }) {
                    Text(PluginBundle.get("open.in.editor"))
                }
            }
        }

        Divider(orientation = Orientation.Horizontal)
        val text = log.prettyPrinted()
        Crossfade(targetState = details, label = "LogDetailsCrossfade") { targetDetails ->
            when {
                log.isComputingDetails || targetDetails == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    val scrollState = rememberScrollState()
                    val background = JewelTheme.globalColors.panelBackground

                    SelectionContainer {
                        Text(
                            text = text ?: "No details available.",
                            modifier = Modifier
                                .background(background)
                                .verticalScroll(scrollState)
                                .padding(12.dp),
                            softWrap = true
                        )
                    }

                    // todo: json语法高亮
                    // CodeArea(
                    //     text = log.prettyPrinted() ?: "No details available.",
                    //     modifier = Modifier.fillMaxSize(),
                    //     language = "json" // 如果是JSON
                    // )
                }
            }
        }
        Divider(orientation = Orientation.Horizontal)
        OutlinedButton({
            MyFileUtil.showJsonInEditor(project, text ?: "")
        }, enabled = isValidJson(text ?: "")) {
            Text(PluginBundle.get("open.in.editor") + "(map eg. debugPrint(jsonEncode({\"hello\": \"flutterx\"})))" )
        }
    }
}

