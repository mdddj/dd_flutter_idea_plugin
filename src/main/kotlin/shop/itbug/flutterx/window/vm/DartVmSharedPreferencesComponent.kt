package shop.itbug.flutterx.window.vm

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import vm.sp.AsyncState
import vm.sp.SharedPreferencesData
import vm.sp.SharedPreferencesServices
import vm.sp.SharedPreferencesState

// dart vm shared preferences 组件
@Composable
fun DartVmSharedPreferencesComponent(project: Project) {
    FlutterAppsTabComponent(project) { app: FlutterAppInstance ->
        SharedPreferencesPanel(app)
    }
}

@Composable
private fun SharedPreferencesPanel(app: FlutterAppInstance) {
    val vmService = app.vmService
    val service = remember(vmService) { SharedPreferencesServices(vmService) }
    val state by service.state.collectAsState()
    val scope = rememberCoroutineScope()

    // 初始化加载
    LaunchedEffect(service) {
        service.fetchAllKeys()
    }

    // 清理
    DisposableEffect(service) {
        onDispose { service.dispose() }
    }

    var splitState by remember { mutableStateOf(SplitLayoutState(0.35f)) }

    HorizontalSplitLayout(
        state = splitState,
        modifier = Modifier.fillMaxSize().border(1.dp, JewelTheme.globalColors.borders.normal),
        first = {
            KeysPanel(
                state = state,
                onRefresh = { scope.launch { service.fetchAllKeys() } },
                onSelectKey = { scope.launch { service.selectKey(it) } },
                onFilter = { service.filter(it) },
                onApiChange = { service.selectApi(it) }
            )
        },
        second = {
            DataPanel(
                state = state,
                onStartEdit = { service.startEditing() },
                onStopEdit = { service.stopEditing() },
                onSave = { key, data -> scope.launch { service.changeValue(key, data) } },
                onDelete = { key -> scope.launch { service.deleteKey(key) } }
            )
        },
        firstPaneMinWidth = 200.dp,
        secondPaneMinWidth = 300.dp
    )
}


// Keys 面板
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeysPanel(
    state: SharedPreferencesState,
    onRefresh: () -> Unit,
    onSelectKey: (String) -> Unit,
    onFilter: (String) -> Unit,
    onApiChange: (Boolean) -> Unit
) {
    var searching by remember { mutableStateOf(false) }
    val searchState = rememberTextFieldState("")
    var debouncedText by remember { mutableStateOf("") }

    LaunchedEffect(searchState.text.toString()) {
        delay(300)
        debouncedText = searchState.text.toString()
        onFilter(debouncedText)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 工具栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = searching,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                TextField(
                    state = searchState,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search keys...") }
                )
            }

            if (!searching) {
                Text("Stored Keys", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
            }

            IconActionButton(
                key = if (searching) AllIconsKeys.Actions.Close else AllIconsKeys.Actions.Search,
                contentDescription = if (searching) "Close" else "Search",
                onClick = {
                    searching = !searching
                    if (!searching) {
                        searchState.setTextAndPlaceCursorAtEnd("")
                        onFilter("")
                    }
                }
            ) {
                Text(if (searching) "Close" else "Search")
            }

            IconActionButton(
                key = AllIconsKeys.Actions.Refresh,
                contentDescription = "Refresh",
                onClick = {
                    searching = false
                    searchState.setTextAndPlaceCursorAtEnd("")
                    onRefresh()
                }
            ) {
                Text("Refresh")
            }
        }

        Divider(Orientation.Horizontal)

        // API 切换
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("API:", modifier = Modifier.alpha(0.7f))
            RadioButtonRow(
                selected = !state.legacyApi,
                onClick = { onApiChange(false) }
            ) {
                Text("Async")
            }
            RadioButtonRow(
                selected = state.legacyApi,
                onClick = { onApiChange(true) }
            ) {
                Text("Legacy")
            }
        }

        Divider(Orientation.Horizontal)

        // Keys 列表
        when (val keysState = state.allKeys) {
            is AsyncState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is AsyncState.Error -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Error: ${keysState.error.message}",
                        color = JewelTheme.globalColors.text.error
                    )
                }
            }

            is AsyncState.Data -> {
                KeysList(
                    keys = keysState.data,
                    selectedKey = state.selectedKey?.key,
                    onSelectKey = onSelectKey
                )
            }
        }
    }
}


// Keys 列表
@Composable
private fun KeysList(
    keys: List<String>,
    selectedKey: String?,
    onSelectKey: (String) -> Unit
) {
    val listState = rememberLazyListState()

    if (keys.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No keys found", modifier = Modifier.alpha(0.6f))
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(keys, key = { it }) { key ->
                KeyItem(
                    key = key,
                    isSelected = key == selectedKey,
                    onClick = { onSelectKey(key) }
                )
            }
        }
    }
}

// 单个 Key 项
@Composable
private fun KeyItem(
    key: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val bgColor = if (isSelected) {
        JewelTheme.globalColors.outlines.focused.copy(alpha = 0.2f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onClick() }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = key,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

// 数据面板
@Composable
private fun DataPanel(
    state: SharedPreferencesState,
    onStartEdit: () -> Unit,
    onStopEdit: () -> Unit,
    onSave: (String, SharedPreferencesData) -> Unit,
    onDelete: (String) -> Unit
) {
    val selectedKey = state.selectedKey

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedKey == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a key to view its data", modifier = Modifier.alpha(0.6f))
            }
        } else {
            // 头部
            DataPanelHeader(
                keyName = selectedKey.key,
                editing = state.editing,
                valueState = selectedKey.value,
                onStartEdit = onStartEdit,
                onStopEdit = onStopEdit,
                onSave = { data -> onSave(selectedKey.key, data) },
                onDelete = { onDelete(selectedKey.key) }
            )

            Divider(Orientation.Horizontal)

            // 内容
            when (val valueState = selectedKey.value) {
                is AsyncState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is AsyncState.Error -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Error: ${valueState.error.message}",
                            color = JewelTheme.globalColors.text.error
                        )
                    }
                }

                is AsyncState.Data -> {
                    DataContent(
                        data = valueState.data,
                        editing = state.editing,
                        onSave = { data -> onSave(selectedKey.key, data) }
                    )
                }
            }
        }
    }
}


// 数据面板头部
@Composable
private fun DataPanelHeader(
    keyName: String,
    editing: Boolean,
    valueState: AsyncState<SharedPreferencesData>,
    onStartEdit: () -> Unit,
    onStopEdit: () -> Unit,
    onSave: (SharedPreferencesData) -> Unit,
    onDelete: () -> Unit
) {
    var currentEditValue by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 重置编辑值
    LaunchedEffect(editing, valueState) {
        if (!editing) {
            currentEditValue = null
        } else if (valueState is AsyncState.Data) {
            currentEditValue = valueState.data.valueAsString
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(keyName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            }
        ) { isEditing ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    OutlinedButton(onClick = onStopEdit) {
                        Text("Cancel")
                    }
                    val data = (valueState as? AsyncState.Data)?.data
                    if (data != null && currentEditValue != null && currentEditValue != data.valueAsString) {
                        DefaultButton(onClick = {
                            currentEditValue?.let { newValue ->
                                val newData = parseNewValue(data, newValue)
                                if (newData != null) {
                                    onSave(newData)
                                }
                            }
                        }) {
                            Text("Apply")
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showDeleteDialog = true }) {
                        Text("Remove")
                    }
                    DefaultButton(onClick = onStartEdit) {
                        Text("Edit")
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            keyName = keyName,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// 解析新值
private fun parseNewValue(original: SharedPreferencesData, newValue: String): SharedPreferencesData? {
    return try {
        when (original) {
            is SharedPreferencesData.StringData -> SharedPreferencesData.StringData(newValue)
            is SharedPreferencesData.IntData -> SharedPreferencesData.IntData(newValue.toInt())
            is SharedPreferencesData.DoubleData -> SharedPreferencesData.DoubleData(newValue.toDouble())
            is SharedPreferencesData.BoolData -> SharedPreferencesData.BoolData(newValue.toBooleanStrict())
            is SharedPreferencesData.StringListData -> {
                // 简单解析，每行一个元素
                val list = newValue.lines().filter { it.isNotBlank() }
                SharedPreferencesData.StringListData(list)
            }
        }
    } catch (e: Exception) {
        null
    }
}

// 删除确认对话框
@Composable
private fun DeleteConfirmDialog(
    keyName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // 简单的确认对话框实现
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(
                    JewelTheme.globalColors.panelBackground,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .border(
                    1.dp,
                    JewelTheme.globalColors.borders.normal,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
                .clickable(enabled = false) { },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Remove Key", fontWeight = FontWeight.Bold)
            Text("Are you sure you want to remove '$keyName'?")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                DefaultButton(onClick = onConfirm) {
                    Text("Remove")
                }
            }
        }
    }
}


// 数据内容
@Composable
private fun DataContent(
    data: SharedPreferencesData,
    editing: Boolean,
    onSave: (SharedPreferencesData) -> Unit
) {
    // 对于 StringListData，直接使用列表值；其他类型使用 valueAsString
    var editValue by remember(data) {
        mutableStateOf(
            when (data) {
                is SharedPreferencesData.StringListData -> data.value.joinToString("\n")
                else -> data.valueAsString
            }
        )
    }
    val scrollState = rememberScrollState()

    // 获取原始值用于比较
    val originalValue = remember(data) {
        when (data) {
            is SharedPreferencesData.StringListData -> data.value.joinToString("\n")
            else -> data.valueAsString
        }
    }

    // 保存逻辑
    val doSave = {
        val newData = parseNewValue(data, editValue)
        if (newData != null) {
            onSave(newData)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 类型显示
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Type:", modifier = Modifier.alpha(0.7f))
            Text(data.kind, fontWeight = FontWeight.Medium)
        }

        // 值显示/编辑
        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            }
        ) { isEditing ->
            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditableValue(
                        data = data,
                        value = editValue,
                        onValueChange = { editValue = it },
                        onEnterPressed = doSave
                    )

                    // 保存按钮 - 当值有变化时显示
                    AnimatedVisibility(
                        visible = editValue != originalValue,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            Text(
                                "Press Enter to save",
                                modifier = Modifier.alpha(0.5f).align(Alignment.CenterVertically)
                            )
                            DefaultButton(onClick = doSave) {
                                Text("Save")
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Value:", modifier = Modifier.alpha(0.7f))
                    ValueDisplay(data)
                }
            }
        }
    }
}

// 值显示
@Composable
private fun ValueDisplay(data: SharedPreferencesData) {
    when (data) {
        is SharedPreferencesData.StringListData -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        JewelTheme.globalColors.borders.normal.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data.value.forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$index:", modifier = Modifier.alpha(0.5f))
                        Text(item)
                    }
                }
                if (data.value.isEmpty()) {
                    Text("(empty list)", modifier = Modifier.alpha(0.5f))
                }
            }
        }

        is SharedPreferencesData.BoolData -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (data.value) Color(0xFF4CAF50) else Color(0xFFF44336),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Text(data.valueAsString, fontWeight = FontWeight.Medium)
            }
        }

        else -> {
            SelectionContainer {
                Text(data.valueAsString)
            }
        }
    }
}

// 可编辑值
@Composable
private fun EditableValue(
    data: SharedPreferencesData,
    value: String,
    onValueChange: (String) -> Unit,
    onEnterPressed: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Value:", modifier = Modifier.alpha(0.7f))

        when (data) {
            is SharedPreferencesData.BoolData -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButtonRow(
                        selected = value == "true",
                        onClick = {
                            onValueChange("true")
                            onEnterPressed()
                        }
                    ) {
                        Text("true")
                    }
                    RadioButtonRow(
                        selected = value == "false",
                        onClick = {
                            onValueChange("false")
                            onEnterPressed()
                        }
                    ) {
                        Text("false")
                    }
                }
            }

            is SharedPreferencesData.StringListData -> {
                // value 已经是纯净的列表值（每行一个），不包含索引
                StringListEditor(
                    items = if (value.isBlank()) emptyList() else value.lines(),
                    onItemsChange = { items -> onValueChange(items.joinToString("\n")) },
                    onEnterPressed = onEnterPressed
                )
            }

            else -> {
                val textState = rememberTextFieldState(value)
                LaunchedEffect(textState.text.toString()) {
                    onValueChange(textState.text.toString())
                }
                TextField(
                    state = textState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                onEnterPressed()
                                true
                            } else {
                                false
                            }
                        }
                )
            }
        }
    }
}


// 字符串列表编辑器
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StringListEditor(
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    onEnterPressed: () -> Unit = {}
) {
    var localItems by remember(items) { mutableStateOf(items.toMutableList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                JewelTheme.globalColors.borders.normal.copy(alpha = 0.1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        localItems.forEachIndexed { index, item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("$index:", modifier = Modifier.alpha(0.5f).width(24.dp))

                val itemState = rememberTextFieldState(item)
                LaunchedEffect(itemState.text.toString()) {
                    val newItems = localItems.toMutableList()
                    newItems[index] = itemState.text.toString()
                    localItems = newItems
                    onItemsChange(newItems)
                }

                TextField(
                    state = itemState,
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                onEnterPressed()
                                true
                            } else {
                                false
                            }
                        }
                )

                IconActionButton(
                    key = AllIconsKeys.General.Remove,
                    contentDescription = "Remove",
                    onClick = {
                        val newItems = localItems.toMutableList()
                        newItems.removeAt(index)
                        localItems = newItems
                        onItemsChange(newItems)
                    }
                ) {
                    Text("Remove item")
                }
            }
        }

        // 添加按钮和保存按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            IconActionButton(
                key = AllIconsKeys.General.Add,
                contentDescription = "Add item",
                onClick = {
                    val newItems = localItems.toMutableList()
                    newItems.add("")
                    localItems = newItems
                    onItemsChange(newItems)
                }
            ) {
                Text("Add item")
            }
        }
    }
}
