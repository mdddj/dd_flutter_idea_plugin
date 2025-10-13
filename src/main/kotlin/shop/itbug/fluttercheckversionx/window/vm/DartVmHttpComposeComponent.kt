package shop.itbug.fluttercheckversionx.window.vm

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.simpleListItemStyle
import shop.itbug.fluttercheckversionx.actions.isValidJson
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInstance
import shop.itbug.fluttercheckversionx.common.jsonToFreezedRun
import shop.itbug.fluttercheckversionx.document.copyTextToClipboard
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.toCurlStringAsDartDevTools
import shop.itbug.fluttercheckversionx.util.ComposeHelper
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.util.RunUtil
import shop.itbug.fluttercheckversionx.util.contextMenu
import shop.itbug.fluttercheckversionx.widget.CustomTabRow
import shop.itbug.fluttercheckversionx.widget.SearchResultCard
import vm.VmService
import vm.network.DartNetworkMonitor
import vm.network.NetworkRequest
import vm.network.RequestStatus
import kotlin.time.Duration.Companion.microseconds


@Composable
fun DartHttpUI(project: Project) {
    FlutterAppsTabComponent(project) {
        AppContentPanel(it, project)
    }
}

@Composable
private fun AppContentPanel(app: FlutterAppInstance, project: Project) {
    val vmService = app.vmService
    val httpRequests = vmService.dartHttpMonitor.requests.collectAsState().value.values.reversed()
    var selectedRequest by remember { mutableStateOf<NetworkRequest?>(null) }
    var outerSplitState by remember { mutableStateOf(SplitLayoutState(0.5f)) }

    LaunchedEffect(vmService) {
        vmService.dartHttpMonitor.startMonitoring()
    }

    HorizontalSplitLayout(
        state = outerSplitState,
        modifier = Modifier.fillMaxWidth().border(1.dp, color = JewelTheme.globalColors.borders.normal),
        first = {
            RequestListPanel(
                vmService = vmService,
                requests = httpRequests,
                selectedRequest = selectedRequest,
                project = project,
                onSelectionChange = {
                    selectedRequest = it
                },
                onStart = {
                    vmService.runInScope {
                        vmService.dartHttpMonitor.startMonitoring(300L)
                    }
                },
                onClean = {
                    vmService.runInScope {
                        dartHttpMonitor.clearRequests()
                    }
                }
            )
        },
        second = {
            RequestDetailPanel(request = selectedRequest, vmService, project)
        },
        firstPaneMinWidth = 300.dp,
        secondPaneMinWidth = 200.dp,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RequestListPanel(
    vmService: VmService,
    requests: List<NetworkRequest>,
    selectedRequest: NetworkRequest?,
    project: Project,
    onSelectionChange: (NetworkRequest) -> Unit,
    onStart: () -> Unit,
    onClean: () -> Unit,
) {

    val isRunning by vmService.dartHttpMonitor.isMonitoring.collectAsState()
    val searchState = rememberTextFieldState("")
    var debouncedSearchText by remember { mutableStateOf("") }
    var showImageRequest by remember { mutableStateOf(false) }
    LaunchedEffect(searchState.text.toString()) {
        delay(300)
        debouncedSearchText = searchState.text.toString()
    }

    //过滤图片请求,和空 path请求
    fun filterImageRequestsAndEmptyPaths(request: List<NetworkRequest>): List<NetworkRequest> {
        val requests = if (showImageRequest) request else request.filter { !it.isLikelyImage }
        return requests.filter { hasMeaningfulPathWithOkHttp(it.uri) }
    }

    val filteredRequests by derivedStateOf {
        val currentList = requests.toList()
        if (debouncedSearchText.isNotBlank()) {
            filterImageRequestsAndEmptyPaths(
                vmService.dartHttpMonitor.filterRequests(
                    containsUrl = debouncedSearchText,
                )
            )
        } else {
            filterImageRequestsAndEmptyPaths(currentList)
        }
    }



    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp, 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val toolTip =
                if (isRunning) PluginBundle.get("compose.dart.vm.listener.stop.icon.button") else PluginBundle.get(
                    "compose.dart.vm.listener.start.icon.button"
                )
            IconActionButton(
                key = if (isRunning) AllIconsKeys.Run.Stop else AllIconsKeys.Debugger.ThreadRunning,
                contentDescription = toolTip,
                onClick = {
                    if (isRunning) {
                        vmService.dartHttpMonitor.stop()
                    } else {
                        onStart()
                    }
                },
            ) {
                Text(toolTip)
            }
            IconActionButton(
                key = AllIconsKeys.General.Delete,
                contentDescription = PluginBundle.get("compose.dart.vm.listener.clean.all"),
                onClick = {
                    onClean()
                }
            ) {
                Text(PluginBundle.get("compose.dart.vm.listener.clean.all"))
            }
            SelectableIconActionButton(
                key = AllIconsKeys.FileTypes.Image,
                contentDescription = "",
                onClick = {
                    showImageRequest = !showImageRequest
                },
                selected = showImageRequest
            ) {
                Text(PluginBundle.get("compose.dart.vm.network.list.showimage"))
            }
            Spacer(Modifier.weight(1f))
            TextField(
                state = searchState,
                placeholder = {
                    Text("${PluginBundle.get("search")}...")
                },
                modifier = Modifier.width(200.dp),
            )
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("${PluginBundle.get("compose.dart.vm.listener.working")}...")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredRequests.ifEmpty { filterImageRequestsAndEmptyPaths(requests.toList()) }) { _, item ->
                    RequestRow(
                        vmService = vmService,
                        request = item,
                        isSelected = item.id == selectedRequest?.id,
                        project = project,
                        searchText = searchState.text.toString(),
                        onClick = { onSelectionChange(item) }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalJewelApi::class)
@Composable
private fun RequestDetailPanel(
    request: NetworkRequest?,
    vmService: VmService,
    project: Project
) {

    if (request == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(PluginBundle.get("compose.dart.vm.listener.select.a.show.details"))
        }
        return
    }

    var detailedRequest by remember(request, request.status, request.responseBody) { mutableStateOf(request) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Headers", "Request Body", "Response Body")


    LaunchedEffect(request) {
        vmService.runInScope {
            val mainIsolatedId = getMainIsolateId()
            if (mainIsolatedId.isNotEmpty()) {
                val detailInfo = request.networkMonitor?.getRequestDetails(request.id)
                if (detailInfo != null) {
                    detailedRequest = detailInfo // 更新 state
                }
            }
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        CustomTabRow(selectedTabIndex, tabs = tabs.map { it }, onTabClick = {
            selectedTabIndex = it
        })
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Box(modifier = Modifier.weight(1f).padding(8.dp)) {
            when (selectedTabIndex) {
                0 -> OverviewTab(detailedRequest, project)
                1 -> HeadersTab(detailedRequest.requestHeaders, detailedRequest.responseHeaders)
                2 -> JsonTextPanel(detailedRequest.requestBody ?: "", project)
                3 -> if (detailedRequest.isLikelyImage) ImageTab(detailedRequest) else JsonTextPanel(
                    detailedRequest.responseBody ?: "",
                    project
                )
            }
        }

    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OverviewTab(request: NetworkRequest, project: Project) {
    fun formatTime(dur: Long): String {
        return DartNetworkMonitor.formatTime(dur)
    }

    val curlText = request.toCurlStringAsDartDevTools()
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyValueText("URL", request.uri)
                KeyValueText("Method", request.method)
                KeyValueText("Status", "") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        request.statusCode?.let { HttpStatusIndicator(it) }
                        Text("${request.statusCode ?: "N/A"} (${request.status})")
                    }
                }
                KeyValueText(
                    "Duration",
                    "${request.duration?.microseconds?.inWholeMilliseconds?.toString() ?: "N/A"} ms"
                )
                KeyValueText("Start Time", formatTime(request.startTime))
                request.endTime?.let { KeyValueText("End Time", formatTime(it)) }
                request.error?.let { KeyValueText("Error", it) }
            }
        }
        Row {
            OutlinedButton(onClick = {
                val gson = GsonBuilder().setPrettyPrinting().create()
                MyFileUtil.showJsonInEditor(project, gson.toJson(request))
            }) { Text(PluginBundle.get("show.raw.data")) }
        }
        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Title("cURL")
        Text(curlText)
        Row {
            IconActionButton(AllIconsKeys.Actions.Copy, contentDescription = "", onClick = {
                curlText.copyTextToClipboard()
            }) {
                Text(PluginBundle.get("copy") + " cURL")
            }
            IconActionButton(AllIconsKeys.Debugger.ThreadRunning, contentDescription = "", onClick = {
                RunUtil.runCommand(project, "cURL", curlText)
            }) {
                Text("Run Command To Terminal")
            }
        }
        Divider(Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
        Title("Events")
        Events(request)
    }
}


@Composable
private fun StatusIndicator(status: RequestStatus, customColor: Color? = null) {
    var color = when (status) {
        RequestStatus.PENDING -> Color.Gray
        RequestStatus.COMPLETED -> Color.Green
        RequestStatus.ERROR -> Color.Red
    }
    if (customColor != null) {
        color = customColor
    }
    when (status) {
        RequestStatus.PENDING -> CircularProgressIndicator(modifier = Modifier.size(8.dp))
        else -> Box(
            modifier = Modifier.size(8.dp).background(color, shape = CircleShape)
        )
    }

}


@Composable
private fun HeadersTab(requestHeaders: Map<String, String>?, responseHeaders: Map<String, String>?) {
    SelectionContainer {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Request Headers", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueTable(requestHeaders)
            Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth().padding(12.dp))
            Text(text = "Response Headers", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueTable(responseHeaders)
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RequestRow(
    vmService: VmService,
    request: NetworkRequest,
    isSelected: Boolean,
    searchText: String,
    project: Project,
    onClick: () -> Unit
) {
    val mainIsolateId = vmService.mainIsolateIdFlow.collectAsState().value
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor = when {
        isSelected -> JewelTheme.simpleListItemStyle.colors.backgroundSelectedActive
        isHovered -> JewelTheme.simpleListItemStyle.colors.backgroundActive
        else -> JewelTheme.simpleListItemStyle.colors.background
    }
    val isOutOfDate = mainIsolateId != request.isolateId
    val outOfDateColor = if (isOutOfDate) JewelTheme.globalColors.text.info else Color.Unspecified
    Box(
        modifier = Modifier.hoverable(interactionSource)
            .pointerHoverIcon(if (isOutOfDate) PointerIcon.Default else PointerIcon.Hand)
            .background(backgroundColor)
            .clickable(
                enabled = !isOutOfDate,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .contextMenu(
                actionGroupId = "dio-window-view-params",
                dataContext = {
                    val lastUpdateRequest = request.networkMonitor?.getRequestDetails(request.id) ?: request
                    val parent = SimpleDataContext.getProjectContext(project)
                    return@contextMenu SimpleDataContext.getSimpleContext(
                        ComposeHelper.networkRequestDataKey,
                        lastUpdateRequest,
                        parent
                    )
                },
                onRightClickable = {
                    onClick.invoke()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusIndicator(request.status, if (isOutOfDate) outOfDateColor else null)
            Text(request.method, modifier = Modifier.width(70.dp), color = outOfDateColor)
            Text(request.statusCode?.toString() ?: "...", modifier = Modifier.width(50.dp), color = outOfDateColor)
            Tooltip(tooltip = { Text("Obsolete") }, enabled = isOutOfDate) {
                SearchResultCard(
                    request.uri, searchQuery = searchText, true, enableAnimation = true, modifier = Modifier.weight(1f),
                    maxLines = 1,
                    color = if (isOutOfDate) JewelTheme.globalColors.text.info else null
                )
            }
            Text(request.duration?.let { "${it.microseconds.inWholeMilliseconds}ms" } ?: "...",
                modifier = Modifier.width(100.dp), fontSize = 11.sp, color = JewelTheme.globalColors.text.info)
            Text(request.endTime?.let { DartNetworkMonitor.formatTime(it) } ?: "...",
                modifier = Modifier.width(150.dp), fontSize = 11.sp, color = JewelTheme.globalColors.text.info)
        }
    }
}

@Composable
private fun KeyValueTable(data: Map<String, String>?) {
    if (data.isNullOrEmpty()) {
        Text(PluginBundle.get("empty"))
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        data.forEach { (key, value) ->
            KeyValueText(key, value)
        }
    }
}

@Composable
private fun ImageTab(request: NetworkRequest) {
    val responseBytes = request.responseByteArray
    val isImage = request.isLikelyImage
    if (responseBytes == null || isImage.not()) {
        Text("None")
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(PluginBundle.get("compose.not.support.image"))
            Row {
                //浏览器中打开
                OutlinedButton({
                    BrowserUtil.browse(request.uri)
                }) {
                    Text(PluginBundle.get("compose.image.show.in.browser"))
                }
            }
        }
    }

}


@Composable
private fun JsonTextPanel(text: String, project: Project) {
    JsonTextPanelWithCoroutine(text, project)
//    val textState = rememberTextFieldState(text)
//    val isJson = isValidJson(textState.text.toString())
//    Column(modifier = Modifier.fillMaxSize().padding(8.dp, 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//            OutlinedButton(onClick = {
//                MyFileUtil.showJsonInEditor(project, text)
//            }, enabled = text.trim().isNotBlank()) { Text(PluginBundle.get("open.in.editor")) }
//            OutlinedButton({
//                project.jsonToFreezedRun(text)
//            }, enabled = text.isNotBlank() && isJson) {
//                Text("Json to freezed")
//            }
//            OutlinedButton({
//                val text = textState.text.toString()
//                val gson = GsonBuilder().setPrettyPrinting().create()
//                val newObject = gson.fromJson(text, JsonObject::class.java)
//                textState.edit {
//                    delete(0, length)
//                    insert(0, gson.toJson(newObject))
//                }
//            }, enabled = isJson) {
//                Text("Format Json")
//            }
//        }
//        TextArea(
//            state = textState,
//            modifier = Modifier.weight(1f).fillMaxSize()
//        )
//    }
}


@Composable
private fun Events(networkRequest: NetworkRequest) {
    val events = networkRequest.events
    SelectionContainer {
        Column {
            for (event in events) {
                KeyValueText(event.event, event.time1)
            }
        }
    }
}

@Composable
private fun KeyValueText(title: String, value: String, valueComp: (@Composable () -> Unit)? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, modifier = Modifier.width(200.dp), color = JewelTheme.globalColors.text.normal)
        if (valueComp != null) {
            valueComp()
        } else {
            Text(value)
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}


/**
 * 一个根据 HTTP 响应状态码显示状态的小圆点组件。
 *
 * @param statusCode HTTP 响应状态码 (例如: 200, 404, 500).
 * @param modifier Modifier 用于自定义布局.
 * @param size 圆点的大小，默认为 8.dp.
 * @param successColor 成功状态 (2xx) 的颜色，默认为绿色.
 * @param failureColor 失败状态 (4xx, 5xx) 的颜色，默认为红色.
 * @param otherColor 其他状态 (1xx, 3xx 等) 的颜色，默认为灰色.
 */
@Composable
fun HttpStatusIndicator(
    statusCode: Int,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    successColor: Color = Color(0xFF4CAF50), // 一个柔和的绿色
    failureColor: Color = Color(0xFFF44336), // 一个柔和的红色
    otherColor: Color = Color.Gray
) {
    val indicatorColor = when (statusCode) {
        in 200..299 -> successColor // 2xx 范围表示成功
        in 400..599 -> failureColor // 4xx 和 5xx 范围表示失败
        else -> otherColor          // 其他所有情况 (如 1xx, 3xx)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape) // 将 Box 裁剪为圆形
            .background(indicatorColor) // 设置背景颜色
    )
}


@Composable
private fun JsonTextPanelWithCoroutine(text: String, project: Project) {
    val textState = rememberTextFieldState()
    val isJson by remember { derivedStateOf { isValidJson(textState.text.toString()) } }
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }
    val scope = rememberCoroutineScope()
    var formatJson: String? by remember { mutableStateOf(null) }

    LaunchedEffect(text) {
        textState.setTextAndPlaceCursorAtEnd(text)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp, 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { MyFileUtil.showJsonInEditor(project, formatJson ?: text) },
                enabled = text.trim().isNotBlank()
            ) {
                Text(PluginBundle.get("open.in.editor"))
            }

            OutlinedButton(
                onClick = { project.jsonToFreezedRun(text) },
                enabled = text.isNotBlank() && isJson
            ) {
                Text("Json to freezed")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        formatJson = formatJsonAsync(textState, gson, textState.text.toString())
                    }
                },
                enabled = isJson
            ) {
                Text("Format Json")
            }
        }

        TextArea(
            state = textState,
            modifier = Modifier.weight(1f).fillMaxSize()
        )
    }
}

private suspend fun formatJsonAsync(textState: TextFieldState, gson: Gson, text: String): String? {
    return try {
        val formattedJson = withContext(Dispatchers.Default) {
            val jsonObject = gson.fromJson(text, JsonObject::class.java)
            gson.toJson(jsonObject)
        }
        textState.setTextAndPlaceCursorAtEnd(formattedJson)
        formattedJson
    } catch (_: Exception) {
        null
    }
}

//检测 path
private fun hasMeaningfulPathWithOkHttp(urlString: String): Boolean {
    val httpUrl = urlString.toHttpUrlOrNull() ?: return false
    val segments = httpUrl.pathSegments
    return segments.size > 1 || (segments.size == 1 && segments.first().isNotEmpty())
}