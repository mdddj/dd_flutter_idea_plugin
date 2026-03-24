package shop.itbug.flutterx.widget_tree

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.tree.rememberTreeState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.window.vm.FlutterAppsTabComponent
import vm.*
import vm.element.WidgetNode

@Composable
fun WidgetTreeWindowContent(project: Project) {
    FlutterAppsTabComponent(project) { app ->
        val scope = rememberCoroutineScope()
        val viewModel = remember(app) { WidgetTreeViewModel(app.vmService, scope, project) }
        
        DisposableEffect(viewModel) {
            onDispose {
                viewModel.dispose()
            }
        }
        
        WidgetTreeView(viewModel, app)
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
fun WidgetTreeView(viewModel: WidgetTreeViewModel, app: FlutterAppInstance) {
    val tree by viewModel.treeState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()
    val searchMatches by viewModel.searchMatches.collectAsState()
    val matchedNodes by viewModel.matchedNodes.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()
    val lazyListState = rememberLazyListState()
    val treeState = rememberTreeState(lazyListState)

    // 自动展开根节点
    LaunchedEffect(tree) {
        if (tree != null) {
            val roots = tree!!.roots
            if (roots.isNotEmpty()) {
                val root = roots.first()
                treeState.openNodes += root.id
            }
        }
    }
    
    // 监听搜索结果变化并自动展开匹配项
    LaunchedEffect(searchMatches) {
        if (searchMatches.isNotEmpty()) {
            treeState.openNodes += searchMatches
        }
    }
    
    // 监听当前搜索索引变化并自动滚到到目标
    LaunchedEffect(currentMatchIndex) {
        if (currentMatchIndex > 0 && matchedNodes.isNotEmpty()) {
            val node = matchedNodes[currentMatchIndex - 1]
            val key = node.valueId ?: "node_${System.identityHashCode(node)}"
            val items = lazyListState.layoutInfo.visibleItemsInfo
            viewModel.selectNode(node)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        InspectorToolbar(app.vmService, viewModel, onRefresh = { viewModel.loadTree() })
        
        Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        
        // 主内容区域：左侧树 + 右侧详情
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: ${error}", color = JewelTheme.globalColors.text.error)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.loadTree() }) {
                    Text("Retry")
                }
            }
        } else if (tree != null) {
            HorizontalSplitLayout(
                modifier = Modifier.fillMaxSize().border(1.dp, JewelTheme.globalColors.borders.normal),
                state = rememberSplitLayoutState(0.55f),
                firstPaneMinWidth = 200.dp,
                secondPaneMinWidth = 200.dp,
                first = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        GroupHeader(text = "Widget Tree", modifier = Modifier.fillMaxWidth())
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyTree(
                                tree = tree!!,
                                treeState = treeState,
                                modifier = Modifier.fillMaxSize().padding(end = 12.dp, top = 4.dp, bottom = 4.dp, start = 4.dp),
                                onElementClick = { element -> viewModel.selectNode(element.data) },
                                onElementDoubleClick = { element ->
                                    viewModel.logger.info("导航到代码${element.data}")
                                    viewModel.jumpToCode(element.data)
                                }
                            ) { element ->
                                val isMatch = searchMatches.contains(element.id)
                                val isCurrentMatch = if (currentMatchIndex > 0 && currentMatchIndex <= matchedNodes.size) {
                                    val currentId = matchedNodes[currentMatchIndex - 1].let { it.valueId ?: "node_${System.identityHashCode(it)}" }
                                    element.id == currentId
                                } else false
                                
                                WidgetNodeRow(
                                    node = element.data, 
                                    isSelected = element.data == selectedNode,
                                    isSearchMatch = isMatch,
                                    isCurrentSearchMatch = isCurrentMatch
                                )
                            }
                            
                            // 垂直滚动条
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(lazyListState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            )
                        }
                    }
                },
                second = {
                    // 右侧：Widget Details
                    WidgetDetailsPanel(viewModel)
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No widget tree data available.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.loadTree() }) {
                        Text("Load Tree")
                    }
                }
            }
        }
    }
}





/**
 * 顶部工具栏，类似 Flutter DevTools Inspector Controls
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalJewelApi::class)
@Composable
private fun InspectorToolbar(vmService: VmService, viewModel: WidgetTreeViewModel, onRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isCompactMode by viewModel.isCompactMode.collectAsState()
    val searchState = rememberTextFieldState(searchQuery)
    val matchedNodes by viewModel.matchedNodes.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()
    var showSearchResultsPopup by remember { mutableStateOf(false) }
    
    // 监听搜索框输入并更新 ViewModel
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text }.collect {
             viewModel.performSearch(it.toString())
        }
    }
    
    // State for toggle buttons
    var slowAnimationsEnabled by remember { mutableStateOf(false) }
    var debugPaintEnabled by remember { mutableStateOf(false) }
    var paintBaselinesEnabled by remember { mutableStateOf(false) }
    var repaintRainbowEnabled by remember { mutableStateOf(false) }
    var selectWidgetModeEnabled by remember { mutableStateOf(false) }
    
    // Load initial states
    LaunchedEffect(Unit) {
        val isolateId = vmService.getMainIsolateId()
        if (isolateId.isNotEmpty()) {
            slowAnimationsEnabled = vmService.getSlowAnimationsEnabled(isolateId)
            debugPaintEnabled = vmService.getDebugPaintEnabled(isolateId)
            paintBaselinesEnabled = vmService.getPaintBaselinesEnabled(isolateId)
            repaintRainbowEnabled = vmService.getRepaintRainbowEnabled(isolateId)
            selectWidgetModeEnabled = vmService.getInspectorOverlayState(isolateId)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Refresh
        Tooltip(tooltip = { Text("Refresh Tree") }) {
            IconActionButton(
                key = AllIconsKeys.Actions.Refresh,
                contentDescription = "Refresh",
                onClick = onRefresh
            )
        }
        
        Divider(orientation = Orientation.Vertical, modifier = Modifier.fillMaxHeight().width(1.dp))
        
        // Select Widget Mode
        Tooltip(tooltip = { Text("Select Widget Mode") }) {
            ToggleableIconActionButton(
                key = AllIconsKeys.General.Locate,
                contentDescription = "Select Widget Mode",
                value = selectWidgetModeEnabled,
                onValueChange = { newValue ->
                    selectWidgetModeEnabled = newValue
                    scope.launch {
                        val isolateId = vmService.getMainIsolateId()
                        if (isolateId.isNotEmpty()) {
                            vmService.setInspectorOverlay(isolateId, newValue)
                        }
                    }
                }
            )
        }
        
        // Slow Animations
        Tooltip(tooltip = { Text("Slow Animations") }) {
            ToggleableIconActionButton(
                key = AllIconsKeys.Actions.ProfileCPU, // Placeholder icon
                contentDescription = "Slow Animations",
                value = slowAnimationsEnabled,
                onValueChange = { newValue ->
                    slowAnimationsEnabled = newValue
                    scope.launch {
                        val isolateId = vmService.getMainIsolateId()
                        if (isolateId.isNotEmpty()) {
                            vmService.toggleSlowAnimations(isolateId, newValue)
                        }
                    }
                }
            )
        }

        // Debug Paint
        Tooltip(tooltip = { Text("Debug Paint") }) {
            ToggleableIconActionButton(
                key = AllIconsKeys.General.TbShown, // Placeholder icon
                contentDescription = "Debug Paint",
                value = debugPaintEnabled,
                onValueChange = { newValue ->
                    debugPaintEnabled = newValue
                    scope.launch {
                        val isolateId = vmService.getMainIsolateId()
                        if (isolateId.isNotEmpty()) {
                            vmService.toggleDebugPaint(isolateId, newValue)
                        }
                    }
                }
            )
        }
        
        // Paint Baselines
        Tooltip(tooltip = { Text("Paint Baselines") }) {
            ToggleableIconActionButton(
                key = AllIconsKeys.Actions.ListFiles, // Valid icon replacement
                contentDescription = "Paint Baselines",
                value = paintBaselinesEnabled,
                onValueChange = { newValue ->
                    paintBaselinesEnabled = newValue
                    scope.launch {
                        val isolateId = vmService.getMainIsolateId()
                        if (isolateId.isNotEmpty()) {
                            vmService.togglePaintBaselines(isolateId, newValue)
                        }
                    }
                }
            )
        }
        
        // Repaint Rainbow
        Tooltip(tooltip = { Text("Repaint Rainbow") }) {
            ToggleableIconActionButton(
                key = AllIconsKeys.Actions.Colors, // Placeholder icon
                contentDescription = "Repaint Rainbow",
                value = repaintRainbowEnabled,
                onValueChange = { newValue ->
                    repaintRainbowEnabled = newValue
                    scope.launch {
                        val isolateId = vmService.getMainIsolateId()
                        if (isolateId.isNotEmpty()) {
                            vmService.toggleRepaintRainbow(isolateId, newValue)
                        }
                    }
                }
            )
        }

        Divider(orientation = Orientation.Vertical, modifier = Modifier.fillMaxHeight().width(1.dp))

        // Compact View Toggle
        Tooltip(tooltip = { Text("Compact View (Flatten single-child wrappers)") }) {
            ToggleableIconActionButton(
                key = AllIconsKeys.General.Filter,
                contentDescription = "Compact View",
                value = isCompactMode,
                onValueChange = { viewModel.toggleCompactMode() }
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        // Search Bar
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextField(
                state = searchState,
                modifier = Modifier.width(200.dp),
                placeholder = { Text("Search widgets...") },
                trailingIcon = {
                    if (searchState.text.isNotEmpty()) {
                         IconActionButton(
                            key = AllIconsKeys.Actions.Close,
                            contentDescription = "Clear Search",
                            onClick = { 
                                searchState.edit { delete(0, length) }
                            }
                        )
                    } else {
                        Icon(key = AllIconsKeys.Actions.Search, contentDescription = "Search")
                    }
                }
            )

            if (matchedNodes.isNotEmpty()) {
                Text(
                    text = "$currentMatchIndex / ${matchedNodes.size}",
                    fontSize = 12.sp,
                    color = JewelTheme.globalColors.text.info
                )
                
                IconActionButton(
                    key = AllIconsKeys.General.ChevronUp,
                    contentDescription = "Previous Match",
                    onClick = { viewModel.searchPrevious() }
                )
                
                IconActionButton(
                    key = AllIconsKeys.General.ChevronDown,
                    contentDescription = "Next Match",
                    onClick = { viewModel.searchNext() }
                )
                
                Box {
                    IconActionButton(
                        key = AllIconsKeys.Actions.ListFiles,
                        contentDescription = "Show Results List",
                        onClick = { showSearchResultsPopup = true }
                    )
                    
                    if (showSearchResultsPopup) {
                        PopupMenu(
                            onDismissRequest = { 
                                showSearchResultsPopup = false
                                true
                            },
                            horizontalAlignment = Alignment.Start
                        ) {
                            matchedNodes.forEach { node ->
                                val key = node.valueId ?: "node_${System.identityHashCode(node)}"
                                val label = "${node.widgetRuntimeType ?: "Widget"} - ${node.description ?: ""}"
                                passiveItem {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.selectSearchResult(key)
                                            showSearchResultsPopup = false
                                        }.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        WidgetTypeIcon(node)
                                        Spacer(Modifier.width(8.dp))
                                        Text(label, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val inspectorManager = vmService.inspectorManager
    val isSelectMode by inspectorManager.overlayState.collectAsState()
    

    // 初始化时加载当前状态
    LaunchedEffect(Unit) {
        val isolateId = vmService.getMainIsolateId()
        if (isolateId.isNotEmpty()) {
            slowAnimationsEnabled = vmService.getSlowAnimationsEnabled(isolateId)
            debugPaintEnabled = vmService.getDebugPaintEnabled(isolateId)
            paintBaselinesEnabled = vmService.getPaintBaselinesEnabled(isolateId)
            repaintRainbowEnabled = vmService.getRepaintRainbowEnabled(isolateId)
        }
    }
}

/**
 * 右侧节点详情面板 - 类似 Flutter DevTools 的属性展示
 */
@Composable
private fun WidgetDetailsPanel(viewModel: WidgetTreeViewModel) {
    val selectedNode by viewModel.selectedNode.collectAsState()
    val properties by viewModel.selectedNodeProperties.collectAsState()
    val isLoadingProperties by viewModel.isLoadingProperties.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 标题带有 Widget 类型
        val headerText = if (selectedNode != null) {
            selectedNode?.widgetRuntimeType ?: "Widget Details"
        } else {
            "Widget Details"
        }
        GroupHeader(text = headerText, modifier = Modifier.fillMaxWidth())
        
        if (selectedNode == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Select a widget to view its properties.",
                    color = JewelTheme.globalColors.text.disabled
                )
            }
        } else if (isLoadingProperties) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Widget 类型标题
                selectedNode?.let { node ->
                    PropertyTreeNode(
                        name = node.widgetRuntimeType ?: "Widget",
                        value = null,
                        isExpanded = true,
                        hasChildren = true,
                        level = 0
                    )
                }
                
                // 显示从 API 获取的属性
                if (properties.isNotEmpty()) {
                    properties.forEach { prop ->
                        PropertyItem(prop, level = 1)
                    }
                } else {
                    // 如果没有详细属性，显示基本信息
                    selectedNode?.let { node ->
                        if (!node.textPreview.isNullOrEmpty()) {
                            PropertyTreeNode(
                                name = "text",
                                value = "\"${node.textPreview}\"",
                                level = 1
                            )
                        }
                        node.creationLocation?.let { loc ->
                            PropertyTreeNode(
                                name = "location",
                                value = "${loc.file}:${loc.line}",
                                level = 1
                            )
                        }
                    }
                }
                
                // Render Object 信息
                selectedNode?.renderObject?.let { renderObject ->
                    Spacer(Modifier.height(8.dp))
                    PropertyTreeNode(
                        name = "renderObject",
                        value = renderObject.description,
                        isExpanded = true,
                        hasChildren = !renderObject.properties.isNullOrEmpty(),
                        level = 0
                    )
                    renderObject.properties?.forEach { prop ->
                        PropertyTreeNode(
                            name = prop.name ?: "",
                            value = prop.description ?: prop.value,
                            level = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 属性项 - 支持 default 标签显示
 */
@Composable
private fun PropertyItem(prop: vm.element.Property, level: Int) {
    val isDefault = prop.level == "debug" || prop.defaultLevel == "debug"
    val valueText = prop.description.ifEmpty { prop.value?.toString() ?: "" }
    
    PropertyTreeNode(
        name = prop.name,
        value = valueText,
        isDefault = isDefault,
        hasChildren = false,
        level = level
    )
}

/**
 * 属性树节点组件 - 类似 Flutter DevTools 的展示样式
 */
@Composable
private fun PropertyTreeNode(
    name: String,
    value: String?,
    isExpanded: Boolean = false,
    hasChildren: Boolean = false,
    isDefault: Boolean = false,
    level: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 展开图标 (如果有子节点)
        if (hasChildren) {
            Icon(
                key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = JewelTheme.globalColors.text.disabled
            )
        } else {
            Spacer(Modifier.width(12.dp))
        }
        
        // 属性名
        Text(
            text = "$name:",
            color = JewelTheme.globalColors.text.info
        )
        
        // 属性值
        if (value != null) {
            Text(
                text = value,
                color = JewelTheme.globalColors.text.normal,
                maxLines = 1
            )
        }
        
        // Default 标签
        if (isDefault) {
            Spacer(Modifier.width(4.dp))
            DefaultBadge()
        }
    }
}

/**
 * Default 标签组件
 */
@Composable
private fun DefaultBadge() {
    Box(
        modifier = Modifier
            .border(
                1.dp,
                JewelTheme.globalColors.borders.normal,
                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "default",
            color = JewelTheme.globalColors.text.disabled,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 10.sp
            )
        )
    }
}

@Composable
fun WidgetNodeRow(
    node: WidgetNode, 
    isSelected: Boolean = false, 
    isSearchMatch: Boolean = false,
    isCurrentSearchMatch: Boolean = false
) {
    val backgroundColor = when {
        isSelected -> JewelTheme.globalColors.panelBackground // 假设选中背景
        isCurrentSearchMatch -> Color(0xFFFF9800).copy(alpha = 0.4f) // 橙色高亮当前匹配
        isSearchMatch -> Color(0xFFFFEB3B).copy(alpha = 0.3f) // 黄色高亮其他匹配
        else -> Color.Transparent
    }
    val forceTextColor = if (isSearchMatch && !isSelected) Color.Black else null
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WidgetTypeIcon(node)
        
        val textColor = forceTextColor ?: when {
            isSelected -> JewelTheme.globalColors.text.normal
            node.children.isNullOrEmpty() -> JewelTheme.globalColors.text.normal
            else -> JewelTheme.globalColors.text.info
        }
        Text(
            text = node.description ?: node.widgetRuntimeType ?: "Widget",
            color = textColor,
            maxLines = 1
        )
        
        if (!node.textPreview.isNullOrEmpty()) {
            Text(
                text = "\"${node.textPreview}\"",
                color = forceTextColor ?: JewelTheme.globalColors.text.disabled,
                maxLines = 1
            )
        }
    }
}

/**
 * Widget 类型图标 - 彩色圆形 + 首字母
 */
@Composable
private fun WidgetTypeIcon(node: WidgetNode) {
    val widgetType = node.widgetRuntimeType ?: node.description ?: "W"
    val firstChar = widgetType.first().uppercaseChar()
    
    // 根据 Widget 类型选择颜色
    val iconColor = getWidgetIconColor(widgetType)
    
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(iconColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = firstChar.toString(),
            color = Color.White,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * 根据 Widget 类型返回对应的图标颜色
 */
private fun getWidgetIconColor(widgetType: String): Color {
    return when {
        // 根节点
        widgetType.contains("root", ignoreCase = true) -> Color(0xFF2196F3) // Blue
        
        // 布局类
        widgetType.contains("Row") || widgetType.contains("Column") ||
        widgetType.contains("Stack") || widgetType.contains("Wrap") ||
        widgetType.contains("Flex") -> Color(0xFF4CAF50) // Green
        
        // 容器类
        widgetType.contains("Container") || widgetType.contains("Padding") ||
        widgetType.contains("Center") || widgetType.contains("Align") ||
        widgetType.contains("SizedBox") || widgetType.contains("Expanded") -> Color(0xFF9C27B0) // Purple
        
        // 文本类
        widgetType.contains("Text") || widgetType.contains("Rich") -> Color(0xFFFF9800) // Orange
        
        // 按钮类
        widgetType.contains("Button") || widgetType.contains("InkWell") ||
        widgetType.contains("GestureDetector") -> Color(0xFFF44336) // Red
        
        // Material 组件
        widgetType.contains("Scaffold") || widgetType.contains("AppBar") ||
        widgetType.contains("Card") || widgetType.contains("Material") -> Color(0xFF00BCD4) // Cyan
        
        // Provider 类
        widgetType.contains("Provider") || widgetType.contains("Consumer") ||
        widgetType.contains("Inherited") -> Color(0xFF607D8B) // Blue Grey
        
        // 滚动类
        widgetType.contains("Scroll") || widgetType.contains("List") ||
        widgetType.contains("Grid") || widgetType.contains("Sliver") -> Color(0xFF795548) // Brown
        
        // 图片类
        widgetType.contains("Image") || widgetType.contains("Icon") -> Color(0xFFE91E63) // Pink
        
        // 默认蓝色
        else -> Color(0xFF2196F3) // Blue
    }
}
