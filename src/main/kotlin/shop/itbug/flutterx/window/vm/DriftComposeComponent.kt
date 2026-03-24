package shop.itbug.flutterx.window.vm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.ide.BrowserUtil
import com.intellij.json.JsonFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.constance.Links
import shop.itbug.flutterx.document.copyTextToClipboard
import shop.itbug.flutterx.i18n.PluginBundle
import vm.drift.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DriftComposeComponent(project: Project) {
    FlutterAppsTabComponent(project) { app: FlutterAppInstance ->
        DriftMainPanel(app, project)
    }
}

@Composable
private fun DriftMainPanel(app: FlutterAppInstance, project: Project) {
    val vmService = app.vmService
    val service = remember(vmService) { DriftServices(vmService) }
    val state by service.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(service) {
        service.fetchDatabases()
    }

    DisposableEffect(service) {
        onDispose { service.dispose() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val outerSplitState = remember { SplitLayoutState(0.2f) }
        val innerSplitState = remember { SplitLayoutState(0.3f) }

        HorizontalSplitLayout(
            state = outerSplitState,
            modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, JewelTheme.globalColors.borders.normal),
            firstPaneMinWidth = 150.dp,
            secondPaneMinWidth = 200.dp,
            first = {
                DatabaseListPanel(
                    databases = state.databases,
                    selectedDb = state.selectedDatabase,
                    onSelectDb = { service.selectDatabase(it) },
                    onRefresh = { scope.launch { service.fetchDatabases() } },
                    onExportDb = { db ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                val bytes = service.exportDatabase(db.id)
                                withContext(Dispatchers.EDT) {
                                    val descriptor =
                                        FileSaverDescriptor("Export Database", "Save database to file", "sqlite")
                                    val saveDialog =
                                        FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
                                    val homePath = System.getProperty("user.home")
                                    val baseDir = LocalFileSystem.getInstance().findFileByPath(homePath)
                                    val wrapper = saveDialog.save(baseDir, "${db.name}.sqlite")
                                    if (wrapper != null) {
                                        WriteCommandAction.runWriteCommandAction(project) {
                                            try {
                                                val file = wrapper.file
                                                file.writeBytes(bytes)
                                                VfsUtil.markDirtyAndRefresh(true, true, true, wrapper.virtualFile)
                                            } catch (ioe: IOException) {

                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // 错误处理
                            }
                        }
                    }
                )
            },
            second = {
                HorizontalSplitLayout(
                    state = innerSplitState,
                    modifier = Modifier.fillMaxSize(),
                    firstPaneMinWidth = 150.dp,
                    secondPaneMinWidth = 200.dp,
                    first = {
                        TableListPanel(
                            selectedDb = state.selectedDatabase,
                            selectedTable = state.selectedTable,
                            onSelectTable = { scope.launch { service.selectTable(it) } }
                        )
                    },
                    second = {
                        DataViewerPanel(
                            selectedTable = state.selectedTable,
                            queryResult = state.queryResult,
                            selectedColumns = state.selectedColumns,
                            filters = state.filters,
                            orderBy = state.orderBy,
                            limit = state.limit,
                            onExecuteSql = { scope.launch { service.executeQuery(state.selectedDatabase!!.id, it) } },
                            onToggleColumn = { service.toggleColumn(it) },
                            onToggleOrderBy = { service.toggleOrderBy(it) },
                            onClearOrderBy = { service.clearOrderBy() },
                            onAddFilter = { service.addFilter(it) },
                            onRemoveFilter = { service.removeFilter(it) },
                            onClearFilters = { service.clearFilters() },
                            onApplyFilters = { service.applyFilters() },
                            onUpdateLimit = { service.updateLimit(it) },
                            onUpdateCellValue = { pkName, pkVal, colName, newVal ->
                                scope.launch {
                                    service.updateCellValue(
                                        state.selectedTable!!.name,
                                        pkName,
                                        pkVal,
                                        colName,
                                        newVal
                                    )
                                }
                            },
                            onDeleteRow = { keyName, value ->
                                scope.launch {
                                    service.deleteData(state.selectedTable!!.name, keyName, value)
                                }
                            },
                            onExportCsv = { result ->
                                scope.launch(Dispatchers.IO) {
                                    val csvContent = result.toCsv()
                                    withContext(Dispatchers.EDT) {
                                        val descriptor =
                                            FileSaverDescriptor("Export CSV", "Save table data to CSV file", "csv")
                                        val saveDialog =
                                            FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
                                        val homePath = System.getProperty("user.home")
                                        val baseDir = LocalFileSystem.getInstance().findFileByPath(homePath)
                                        val wrapper = saveDialog.save(baseDir, "${state.selectedTable!!.name}.csv")
                                        if (wrapper != null) {
                                            WriteCommandAction.runWriteCommandAction(project) {
                                                try {
                                                    val file = wrapper.file
                                                    file.writeText(csvContent)
                                                    VfsUtil.markDirtyAndRefresh(true, true, true, wrapper.virtualFile)
                                                } catch (ioe: IOException) {
                                                    // handle error
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onPreviewCsv = { result ->
                                result.openInEditor(project)
                            },
                            project = project
                        )
                    },
                )
            },
        )

        // Bottom log bar
        StatusBar(state.logs)
    }
}

/**
 * 数据库列表
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatabaseListPanel(
    databases: DriftAsyncState<List<DriftDatabase>>,
    selectedDb: DriftDatabase?,
    onSelectDb: (DriftDatabase) -> Unit,
    onRefresh: () -> Unit,
    onExportDb: (DriftDatabase) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(PluginBundle.get("drift.databases"), fontWeight = FontWeight.Bold)
            Row {
                IconActionButton(
                    key = AllIconsKeys.Actions.Help,
                    contentDescription = PluginBundle.get("doc"),
                    onClick = {
                        BrowserUtil.browse(Links.DriftDocument)
                    }
                ) {
                    Text(PluginBundle.get("doc"))
                }
                IconActionButton(
                    key = AllIconsKeys.Actions.Refresh,
                    contentDescription = PluginBundle.get("drift.refresh"),
                    onClick = onRefresh
                ) {
                    Text(PluginBundle.get("drift.refresh"))
                }
            }
        }
        Divider(Orientation.Horizontal)

        when (databases) {
            is DriftAsyncState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is DriftAsyncState.Error -> Box(
                Modifier.fillMaxSize().padding(8.dp)
            ) {
                Text(
                    "${PluginBundle.get("drift.error")}: ${databases.error.message}",
                    color = JewelTheme.globalColors.text.error
                )
            }

            is DriftAsyncState.Data -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(databases.data) { db ->
                        SelectableItem(
                            label = db.name,
                            isSelected = db == selectedDb,
                            onClick = { onSelectDb(db) },
                            trailingContent = {
                                IconActionButton(
                                    key = AllIconsKeys.Actions.Download,
                                    contentDescription = PluginBundle.get("drift.export"),
                                    onClick = { onExportDb(db) }
                                ) {
                                    Text(PluginBundle.get("drift.export"))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 表列表
 */
@Composable
private fun TableListPanel(
    selectedDb: DriftDatabase?,
    selectedTable: DriftTable?,
    onSelectTable: (DriftTable) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(PluginBundle.get("drift.tables"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        Divider(Orientation.Horizontal)

        if (selectedDb == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    PluginBundle.get("drift.select.db"),
                    modifier = Modifier.alpha(0.6f)
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(selectedDb.tables) { table ->
                    SelectableItem(
                        label = table.name,
                        isSelected = table == selectedTable,
                        onClick = { onSelectTable(table) }
                    )
                }
            }
        }
    }
}


/**
 * 数据展示区域
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DataViewerPanel(
    selectedTable: DriftTable?,
    queryResult: DriftAsyncState<DriftQueryResult>?,
    selectedColumns: Set<String>,
    filters: List<DriftFilter>,
    orderBy: List<DriftOrderBy>,
    limit: Int,
    onExecuteSql: (String) -> Unit,
    onToggleColumn: (String) -> Unit,
    onToggleOrderBy: (String) -> Unit,
    onClearOrderBy: () -> Unit,
    onAddFilter: (DriftFilter) -> Unit,
    onRemoveFilter: (DriftFilter) -> Unit,
    onClearFilters: () -> Unit,
    onApplyFilters: () -> Unit,
    onUpdateLimit: (Int) -> Unit,
    onDeleteRow: (String, Any) -> Unit,
    onUpdateCellValue: (String, Any, String, String) -> Unit,
    onExportCsv: (DriftQueryResult) -> Unit,
    onPreviewCsv: (DriftQueryResult) -> Unit,
    project: Project
) {
    var showColumnsPopup by remember { mutableStateOf(false) }
    var showFilterBuilder by remember { mutableStateOf(false) }
    var showLimitPopup by remember { mutableStateOf(false) }

    val columnWidths = remember(selectedTable?.name) { mutableStateMapOf<String, Dp>() }

    Column(modifier = Modifier.fillMaxSize().animateContentSize()) {
        if (selectedTable == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("drift.select.table"), modifier = Modifier.alpha(0.6f))
            }
        } else {
            val sqlState = rememberTextFieldState("SELECT * FROM ${selectedTable.name}")
            Column(modifier = Modifier.padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(sqlState, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    DefaultButton(onClick = { onExecuteSql(sqlState.text.toString()) }) {
                        Text(PluginBundle.get("drift.execute.sql"))
                    }
                    Spacer(Modifier.width(8.dp))

                    Box {
                        IconActionButton(
                            key = AllIconsKeys.General.ExternalTools,
                            contentDescription = PluginBundle.get("drift.columns"),
                            onClick = { showColumnsPopup = !showColumnsPopup }
                        ) {
                            Text(PluginBundle.get("drift.columns"))
                        }

                        if (showColumnsPopup) {
                            ColumnSelectionPopup(
                                columns = selectedTable.columns,
                                selectedColumns = selectedColumns,
                                onToggleColumn = onToggleColumn,
                                onDismiss = { showColumnsPopup = false }
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box {
                        IconActionButton(
                            key = AllIconsKeys.General.Filter,
                            contentDescription = PluginBundle.get("drift.add.filter"),
                            onClick = { showFilterBuilder = !showFilterBuilder }
                        ) {
                            Text(PluginBundle.get("drift.add.filter"))
                        }

                        if (showFilterBuilder) {
                            FilterBuilderPopup(
                                columns = selectedTable.columns,
                                onAddFilter = {
                                    onAddFilter(it)
                                    showFilterBuilder = false
                                },
                                onDismiss = { showFilterBuilder = false }
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box {
                        IconActionButton(
                            key = AllIconsKeys.General.Settings,
                            contentDescription = PluginBundle.get("drift.settings"),
                            onClick = { showLimitPopup = !showLimitPopup }
                        ) {
                            Text(PluginBundle.get("drift.settings"))
                        }

                        if (showLimitPopup) {
                            SettingsPopup(
                                limit = limit,
                                onUpdateLimit = onUpdateLimit,
                                onDismiss = { showLimitPopup = false }
                            )
                        }
                    }

                    if (queryResult is DriftAsyncState.Data) {
                        Spacer(Modifier.width(8.dp))
                        IconActionButton(
                            key = AllIconsKeys.ToolbarDecorator.Export,
                            contentDescription = PluginBundle.get("drift.export.csv"),
                            onClick = { onExportCsv(queryResult.data) }
                        ) {
                            Text(PluginBundle.get("drift.export.csv"))
                        }

                        Spacer(Modifier.width(8.dp))
                        IconActionButton(
                            key = AllIconsKeys.Actions.Preview,
                            contentDescription = PluginBundle.get("drift.preview.csv"),
                            onClick = { onPreviewCsv(queryResult.data) }
                        ) {
                            Text(PluginBundle.get("drift.preview.csv"))
                        }
                    }
                }

                AnimatedVisibility(visible = filters.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f)).padding(8.dp)
                    ) {
                        //
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${PluginBundle.get("drift.active.filters")}:", fontWeight = FontWeight.Bold)

                            filters.forEach { filter ->
                                Row(
                                    modifier = Modifier
                                        .background(JewelTheme.globalColors.panelBackground, RoundedCornerShape(4.dp))
                                        .border(1.dp, JewelTheme.globalColors.borders.normal, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${filter.columnName} ${filter.operator.label} ${filter.value}")
                                    Spacer(Modifier.width(4.dp))
                                    IconActionButton(
                                        key = AllIconsKeys.General.Close,
                                        contentDescription = "Remove Filter",
                                        onClick = { onRemoveFilter(filter) }
                                    )
                                }
                            }

                            OutlinedButton(onClick = onClearFilters) { Text(PluginBundle.get("drift.clear.all")) }
                            DefaultButton(onClick = onApplyFilters) { Text(PluginBundle.get("drift.apply")) }
                        }
                    }
                }

                AnimatedVisibility(visible = orderBy.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            .background(JewelTheme.globalColors.panelBackground.copy(alpha = 0.3f)).padding(8.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${PluginBundle.get("drift.order.by")}:", fontWeight = FontWeight.Bold)

                            orderBy.forEach { order ->
                                Row(
                                    modifier = Modifier
                                        .background(JewelTheme.globalColors.panelBackground, RoundedCornerShape(4.dp))
                                        .border(1.dp, JewelTheme.globalColors.borders.normal, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${order.columnName} ${if (order.isAscending) "ASC" else "DESC"}")
                                    Spacer(Modifier.width(4.dp))
                                    IconActionButton(
                                        key = AllIconsKeys.General.Close,
                                        contentDescription = "Remove Sort",
                                        onClick = { onToggleOrderBy(order.columnName) }
                                    )
                                }
                            }

                            OutlinedButton(onClick = onClearOrderBy) { Text(PluginBundle.get("drift.clear.all")) }
                        }
                    }
                }
            }
            Divider(Orientation.Horizontal)

            Crossfade(targetState = queryResult, label = "DataTransition") { result ->
                when (result) {
                    is DriftAsyncState.Loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    is DriftAsyncState.Error -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            "${PluginBundle.get("drift.query.error")}: ${result.error.message}",
                            color = JewelTheme.globalColors.text.error
                        )
                    }

                    is DriftAsyncState.Data -> {
                        ResultTable(
                            project = project,
                            tableName = selectedTable.name,
                            result = result.data,
                            orderBy = orderBy,
                            columnWidths = columnWidths,
                            onToggleOrderBy = onToggleOrderBy,
                            onDelete = { row ->
                                val firstCol = result.data.columns.firstOrNull()
                                if (firstCol != null) {
                                    row.data[firstCol]?.let { onDeleteRow(firstCol, it) }
                                }
                            },
                            onUpdateCell = onUpdateCellValue
                        )
                    }

                    null -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text(PluginBundle.get("drift.ready")) }
                }
            }
        }
    }
}
@Composable
private fun ResultTable(
    project: Project,
    tableName: String,
    result: DriftQueryResult,
    orderBy: List<DriftOrderBy>,
    columnWidths: MutableMap<String, Dp>,
    onToggleOrderBy: (String) -> Unit,
    onDelete: (DriftRow) -> Unit,
    onUpdateCell: (String, Any, String, String) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    // --- 颜色定义 ---
    val isDark = JewelTheme.isDark
    val borderColor = if (isDark) Color(0xFF4E5157) else Color(0xFFD1D1D1)
    val stripeColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)

    val cellHorizontalPadding = 8.dp

    LaunchedEffect(result.columns) {
        result.columns.forEach { col ->
            if (!columnWidths.containsKey(col)) {
                columnWidths[col] = 150.dp
            }
        }
    }

    // 表格最外层边框
    Box(Modifier.fillMaxSize().border(1.dp, borderColor)) {
        Column(Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)) {

            // --- 表头 ---
            Row(
                Modifier
                    .background(JewelTheme.globalColors.panelBackground)
                    .height(IntrinsicSize.Min)
                    .drawBehind {
                        // 表头底部的横线
                        drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                    }
            ) {
                result.columns.forEach { col ->
                    val sort = orderBy.find { it.columnName == col }
                    val currentWidth = columnWidths[col] ?: 150.dp

                    Row(modifier = Modifier.width(currentWidth).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onToggleOrderBy(col) }
                                .padding(horizontal = cellHorizontalPadding, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(col, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (sort != null) {
                                Icon(
                                    if (sort.isAscending) AllIconsKeys.General.ArrowUp else AllIconsKeys.General.ArrowDown,
                                    contentDescription = "Sort Icon",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // 列与列之间的竖线
                        Box(Modifier.fillMaxHeight().width(1.dp).background(borderColor)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR)))
                            .pointerInput(col) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newWidth = (columnWidths[col] ?: 150.dp) + with(density) { dragAmount.toDp() }
                                    columnWidths[col] = newWidth.coerceAtLeast(50.dp)
                                }
                            }
                        )
                    }
                }

                // 【修改点】操作列的表头也加上右侧竖线
                if (result.rows.isNotEmpty()) {
                    Row(modifier = Modifier.width(100.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).padding(horizontal = cellHorizontalPadding, vertical = 8.dp)) {
                            Text(PluginBundle.get("drift.actions"), fontWeight = FontWeight.Bold)
                        }
                        // 操作列最右侧的竖线
                        Box(Modifier.fillMaxHeight().width(1.dp).background(borderColor))
                    }
                }
            }

            // --- 数据内容 ---
            Box(Modifier.weight(1f)) {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(result.rows) { index, row ->
                        val isEven = index % 2 == 0
                        val rowBg = if (isEven) Color.Transparent else stripeColor

                        Row(
                            Modifier
                                .background(rowBg)
                                .height(IntrinsicSize.Min)
                                .drawBehind {
                                    // 每一行底部的横线
                                    drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                                }
                        ) {
                            for (col in result.columns) {
                                val currentWidth = columnWidths[col] ?: 150.dp

                                Box(modifier = Modifier.width(currentWidth).fillMaxHeight()) {
                                    DataCell(
                                        project = project,
                                        value = row.data[col],
                                        columnName = col,
                                        width = currentWidth,
                                        horizontalPadding = cellHorizontalPadding,
                                        onEdit = { newValue ->
                                            val firstCol = result.columns.firstOrNull()
                                            if (firstCol != null) {
                                                row.data[firstCol]?.let { pkVal ->
                                                    onUpdateCell(firstCol, pkVal, col, newValue)
                                                }
                                            }
                                        }
                                    )
                                    // 单元格右侧竖线
                                    Box(Modifier.fillMaxHeight().width(1.dp).background(borderColor).align(Alignment.CenterEnd))
                                }
                            }

                            // 【修改点】操作按钮这一格也加上右侧竖线
                            if (result.rows.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.width(100.dp).fillMaxHeight(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        IconActionButton(
                                            key = AllIconsKeys.General.Delete,
                                            contentDescription = PluginBundle.get("delete_base_text"),
                                            onClick = {
                                                onDelete(row)
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )

                                    }
                                    // 操作列最右侧的竖线
                                    Box(Modifier.fillMaxHeight().width(1.dp).background(borderColor))
                                }
                            }
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(lazyListState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }
        }

        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalScrollState),
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp)
        )
    }
}

@Composable
private fun SelectableItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val bgColor = if (isSelected) JewelTheme.globalColors.outlines.focused.copy(alpha = 0.2f) else Color.Transparent
    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            trailingContent()
        }
    }
}


//底部显示一些 log和状态.
@Composable
private fun StatusBar(logs: List<String>) {
    val lastLog = logs.lastOrNull() ?: "Ready"
    Row(
        modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AllIconsKeys.General.BalloonInformation, contentDescription = "Log")
        Spacer(Modifier.width(8.dp))
        SelectionContainer {
            Text(lastLog, modifier = Modifier.alpha(0.7f).animateContentSize())
        }
    }
}

@Composable
private fun ColumnSelectionPopup(
    columns: List<DriftColumn>,
    selectedColumns: Set<String>,
    onToggleColumn: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        offset = androidx.compose.ui.unit.IntOffset(0, 40)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .border(1.dp, JewelTheme.globalColors.borders.normal)
                .padding(8.dp)
        ) {
            Column {
                columns.forEach { col ->
                    val isChecked = selectedColumns.contains(col.name)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggleColumn(col.name) }.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isChecked, onCheckedChange = { onToggleColumn(col.name) })
                        Spacer(Modifier.width(8.dp))
                        Text(col.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBuilderPopup(
    columns: List<DriftColumn>,
    onAddFilter: (DriftFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColumn by remember { mutableStateOf(columns.firstOrNull()) }
    var selectedOperator by remember { mutableStateOf(DriftFilterOperator.Equals) }
    var filterValue by remember { mutableStateOf("") }

    val operators = remember(selectedColumn) {
        val type = selectedColumn?.type?.uppercase() ?: ""
        when {
            type.contains("INT") || type.contains("DOUBLE") || type.contains("REAL") || type.contains("NUM") ->
                listOf(
                    DriftFilterOperator.Equals,
                    DriftFilterOperator.GreaterThan,
                    DriftFilterOperator.LessThan,
                    DriftFilterOperator.Contains
                )

            type.contains("BOOL") ->
                listOf(DriftFilterOperator.IsYes, DriftFilterOperator.IsNo)

            type.contains("DATE") || type.contains("TIME") ->
                listOf(DriftFilterOperator.OnDate, DriftFilterOperator.Before, DriftFilterOperator.After)

            else ->
                listOf(DriftFilterOperator.Equals, DriftFilterOperator.Contains)
        }
    }

    LaunchedEffect(operators) {
        if (!operators.contains(selectedOperator)) {
            selectedOperator = operators.firstOrNull() ?: DriftFilterOperator.Equals
        }
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        offset = IntOffset(0, 40)
    ) {
        Box(
            modifier = Modifier
                .width(350.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .border(1.dp, JewelTheme.globalColors.borders.normal)
                .padding(12.dp)
                .animateContentSize()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(PluginBundle.get("drift.add.new.filter"), fontWeight = FontWeight.Bold)

                // Column Selection
                Text("${PluginBundle.get("drift.column")}:", modifier = Modifier.alpha(0.7f))
                LazyColumn(Modifier.height(120.dp).border(1.dp, JewelTheme.globalColors.borders.normal)) {
                    items(columns) { col ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selectedColumn = col }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedColumn == col, onClick = { selectedColumn = col })
                            Spacer(Modifier.width(8.dp))
                            Text(col.name, modifier = Modifier.weight(1f))
                            Text(col.type, color = Color.Gray, modifier = Modifier.alpha(0.6f))
                        }
                    }
                }

                Divider(Orientation.Horizontal)

                Text("${PluginBundle.get("drift.condition")}:", modifier = Modifier.alpha(0.7f))
                FlowRow(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    operators.forEach { op ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedOperator = op }) {
                            RadioButton(selected = selectedOperator == op, onClick = { selectedOperator = op })
                            Spacer(Modifier.width(4.dp))
                            Text(op.label)
                        }
                    }
                }

                // Value Input
                if (selectedOperator != DriftFilterOperator.IsYes && selectedOperator != DriftFilterOperator.IsNo) {
                    Text("${PluginBundle.get("drift.value")}:", modifier = Modifier.alpha(0.7f))
                    val textFieldState = rememberTextFieldState(filterValue)
                    TextField(textFieldState, modifier = Modifier.fillMaxWidth())
                    LaunchedEffect(textFieldState.text) { filterValue = textFieldState.text.toString() }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(PluginBundle.get("cancel")) }
                    Spacer(Modifier.width(8.dp))
                    DefaultButton(onClick = {
                        selectedColumn?.let {
                            onAddFilter(DriftFilter(it.name, it.type, selectedOperator, filterValue))
                        }
                    }) {
                        Text(PluginBundle.get("drift.add"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPopup(
    limit: Int,
    onUpdateLimit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var limitValue by remember { mutableStateOf(limit.toString()) }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        offset = IntOffset(0, 40)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .border(1.dp, JewelTheme.globalColors.borders.normal)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(PluginBundle.get("drift.query.settings"), fontWeight = FontWeight.Bold)

                Text("${PluginBundle.get("drift.limit.rows")}:", modifier = Modifier.alpha(0.7f))
                val textFieldState = rememberTextFieldState(limitValue)
                TextField(textFieldState, modifier = Modifier.fillMaxWidth())

                LaunchedEffect(textFieldState.text) {
                    limitValue = textFieldState.text.toString()
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DefaultButton(onClick = {
                        limitValue.toIntOrNull()?.let { onUpdateLimit(it) }
                        onDismiss()
                    }) {
                        Text(PluginBundle.get("drift.apply"))
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DataCell(
    project: Project,
    value: Any?,
    columnName: String,
    width: Dp,
    horizontalPadding: Dp,
    onEdit: (String) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditPopup by remember { mutableStateOf(false) }
    var showingAsDate by remember { mutableStateOf(false) }

    val displayValue = remember(value, showingAsDate) {
        if (showingAsDate && value != null) {
            try {
                val timestamp = value.toString().toLong()
                val date = Date(if (timestamp > 10000000000L) timestamp else timestamp * 1000)
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date)
            } catch (e: Exception) {
                value.toString()
            }
        } else {
            value?.toString() ?: "NULL"
        }
    }

    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        SelectionContainer {
            Text(
                text = displayValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(end = 24.dp)
            )
        }

        if (isHovered || showMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .background(JewelTheme.globalColors.panelBackground)
            ) {
                IconActionButton(
                    key = AllIconsKeys.Actions.MoreHorizontal,
                    contentDescription = "Cell Actions",
                    onClick = { showMenu = !showMenu }
                )

                if (showMenu) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = IntOffset(0, 30),
                        onDismissRequest = { showMenu = false },
                        properties = PopupProperties(focusable = true),
                        content = {
                            CellDropdownMenu(
                                showingAsDate = showingAsDate,
                                hasTimestamp = value?.toString()?.toLongOrNull() != null,
                                onAction = { action ->
                                    showMenu = false
                                    when (action) {
                                        "edit" -> showEditPopup = true
                                        "text" -> openInEditor(project, value?.toString() ?: "", false)
                                        "json" -> openInEditor(project, value?.toString() ?: "", true)
                                        "toggleDate" -> showingAsDate = !showingAsDate
                                        "copy" -> (value?.toString() ?: "").copyTextToClipboard()
                                    }
                                }
                            )
                        })
                }
            }
        }

        if (showEditPopup) {
            EditCellPopup(
                value = value?.toString() ?: "",
                onConfirm = {
                    onEdit(it)
                    showEditPopup = false
                },
                onDismiss = { showEditPopup = false }
            )
        }
    }
}

@Composable
private fun CellDropdownMenu(
    showingAsDate: Boolean,
    hasTimestamp: Boolean,
    onAction: (String) -> Unit
) {
    Box(
        Modifier
            .background(JewelTheme.globalColors.panelBackground)
            .border(1.dp, JewelTheme.globalColors.borders.normal)
            .padding(4.dp)
            .width(IntrinsicSize.Max)
    ) {
        Column {
            SelectableItem(PluginBundle.get("copy"), isSelected = false, onClick = { onAction("copy") })
            SelectableItem(PluginBundle.get("drift.cell.edit"), isSelected = false, onClick = { onAction("edit") })
            SelectableItem(
                PluginBundle.get("drift.cell.open.editor"),
                isSelected = false,
                onClick = { onAction("text") })
            SelectableItem(PluginBundle.get("drift.cell.open.json"), isSelected = false, onClick = { onAction("json") })
            if (hasTimestamp) {
                SelectableItem(
                    if (showingAsDate) PluginBundle.get("drift.cell.show.timestamp") else PluginBundle.get("drift.cell.show.date"),
                    isSelected = false,
                    onClick = { onAction("toggleDate") }
                )
            }
        }
    }
}

@Composable
private fun EditCellPopup(
    value: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(value) }
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
        offset = IntOffset(0, 40)
    ) {
        Box(
            Modifier
                .width(300.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .border(1.dp, JewelTheme.globalColors.borders.normal)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(PluginBundle.get("drift.edit.value"), fontWeight = FontWeight.Bold)
                val state = rememberTextFieldState(textValue)
                TextArea(
                    state,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.MultiLine(3, 6),
                    placeholder = { Text(PluginBundle.get("drift.new.value")) })
                LaunchedEffect(state.text) { textValue = state.text.toString() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text(PluginBundle.get("cancel")) }
                    Spacer(Modifier.width(8.dp))
                    DefaultButton(onClick = { onConfirm(textValue) }) { Text(PluginBundle.get("drift.save")) }
                }
            }
        }
    }
}


private fun openInEditor(project: Project, content: String, isJson: Boolean) {
    ApplicationManager.getApplication().invokeLater {
        val fileName = if (isJson) "cell_content.json" else "cell_content.txt"
        val fileType = if (isJson) JsonFileType.INSTANCE else PlainTextFileType.INSTANCE
        val virtualFile = LightVirtualFile(fileName, fileType, content)
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }
}
