package shop.itbug.fluttercheckversionx.window

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.editorTabStyle
import org.jetbrains.jewel.ui.theme.simpleListItemStyle
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInstance
import shop.itbug.fluttercheckversionx.common.dart.FlutterXVMService
import shop.itbug.fluttercheckversionx.common.dart.isSupportDartVm
import shop.itbug.fluttercheckversionx.util.ComposeHelper
import shop.itbug.fluttercheckversionx.util.contextMenu
import shop.itbug.fluttercheckversionx.widget.JsonEditorTextPanel
import vm.VmService
import vm.network.DartNetworkMonitor
import vm.network.NetworkRequest
import vm.network.RequestStatus
import java.awt.Dimension
import javax.swing.SwingUtilities

// dart http
class DartHttpWindowsFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        toolWindow.addComposeTab("Http 监听") {
            DartHttpUI(project)
//            SwingBridgeTheme {
//                var selectedRequest by remember { mutableStateOf<NetworkRequest?>(null) }
//                RequestListPanel(
//                    requests = generateMockData(),
//                    selectedRequest = selectedRequest,
//                    project = project,
//                    onSelectionChange = {
//                        selectedRequest = it
//                    },
//                )
//            }
        }
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return isSupportDartVm
    }
}


@OptIn(ExperimentalJewelApi::class)
@Composable
private fun DartHttpUI(project: Project) {
    val flutterAppService = FlutterXVMService.getInstance(project)
    val runningApps by flutterAppService.runningApps.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabIds by remember(runningApps) { mutableStateOf(runningApps.map { it.appInfo.appId }.toList()) }


    val tabs = remember(tabIds, selectedTabIndex) {
        tabIds.mapIndexed { index, _ ->
            TabData.Default(
                selected = index == selectedTabIndex,
                content = { tabState ->
                    SimpleTabContent("hello", tabState)
                }
            )
        }
    }


    SwingBridgeTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TabStrip(tabs, JewelTheme.editorTabStyle)
            val selectApp = runningApps.getOrNull(selectedTabIndex)
            if (selectApp != null) {
                AppContentPanel(selectApp, project)
            }
        }
    }
}

@Composable
private fun AppContentPanel(app: FlutterAppInstance, project: Project) {
    val vmService = app.vmService
    val httpRequests =
        remember(vmService.dartHttpMonitor) { mutableStateListOf(*vmService.getAllRequests.toTypedArray()) }
    var selectedRequest by remember { mutableStateOf<NetworkRequest?>(null) }
    var outerSplitState by remember { mutableStateOf(SplitLayoutState(0.5f)) }

    fun updateRequest(request: NetworkRequest) {
        val index = httpRequests.indexOfFirst { it.id == request.id }
        if (index != -1) {
            httpRequests[index] = request
        }
    }

    val requestListener = DartNetworkMonitor.DefaultNetworkRequestListener(
        started = httpRequests::addFirst,
        update = ::updateRequest,
        completed = ::updateRequest
    )

    LaunchedEffect(vmService.dartHttpMonitor) {
        vmService.startMonitoring(
            listener = requestListener
        )
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
                        vmService.startMonitoring(listener = requestListener)
                    }
                },
                onClean = {
                    httpRequests.clear()
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

@Composable
private fun RequestListPanel(
    vmService: VmService,
    requests: List<NetworkRequest>,
    selectedRequest: NetworkRequest?,
    project: Project,
    onSelectionChange: (NetworkRequest) -> Unit,
    onStart: () -> Unit,
    onClean:()->Unit
) {
    val isRunning by vmService.dartHttpIsMonitoring.collectAsState()
    val searchState = rememberTextFieldState("")
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp, 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(
                key = if (isRunning) AllIconsKeys.Run.Stop else AllIconsKeys.Debugger.ThreadRunning,
                contentDescription = if (isRunning) "正在监听,点击停止监听" else "开始监听 http请求",
                onClick = {
                    if (isRunning) {
                        vmService.destroyHttpMonitor()
                    } else {
                        onStart()
                    }
                },
            )
            IconActionButton(
                key = AllIconsKeys.General.Delete,
                contentDescription = "清空全部",
                onClick = {
                    onClean()
                }
            )
            Spacer(Modifier.weight(1f))
            TextField(
                state = searchState,
                placeholder = {
                    Text("Search...")
                },
                modifier = Modifier.width(200.dp)
            )
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在监听网络请求...")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(requests) { _, item ->
                    RequestRow(
                        request = item,
                        isSelected = item.id == selectedRequest?.id,
                        project = project,
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
            Text("Select a request to see details")
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
        TabStrip(
            tabs = tabs.mapIndexed { index, title ->
                TabData.Default(
                    selected = index == selectedTabIndex,
                    onClick = { selectedTabIndex = index },
                    content = { Text(title) }
                )
            },
            style = JewelTheme.editorTabStyle
        )
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Box(modifier = Modifier.weight(1f).padding(8.dp)) {
            when (selectedTabIndex) {
                0 -> OverviewTab(detailedRequest)
                1 -> HeadersTab(detailedRequest.requestHeaders, detailedRequest.responseHeaders)
                2 -> BodyTab(null, detailedRequest.requestBody, project)
                3 -> BodyTab(null, detailedRequest.responseBody, project)
            }
        }

    }
}

@Composable
private fun OverviewTab(request: NetworkRequest) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("URL: ${request.uri}")
        Text("Method: ${request.method}")
        Text("Status: ${request.statusCode ?: "N/A"} (${request.status})")
        Text("Duration: ${request.duration?.toString() ?: "N/A"} ms")
        Text("Start Time: ${request.startTime}")
        request.error?.let { Text("Error: $it", color = Color.Red) }
    }
}

@Composable
private fun StatusIndicator(status: RequestStatus) {
    val color = when (status) {
        RequestStatus.PENDING -> Color.Gray
        RequestStatus.COMPLETED -> Color.Green
        RequestStatus.ERROR -> Color.Red
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
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f)) {
            Text("Request Headers")
            KeyValueTable(requestHeaders)
        }
        Column(Modifier.weight(1f)) {
            Text("Response Headers")
            KeyValueTable(responseHeaders)
        }
    }
}

@Composable
private fun RequestRow(request: NetworkRequest, isSelected: Boolean, project: Project, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor = when {
        isSelected -> JewelTheme.simpleListItemStyle.colors.backgroundSelectedActive
        isHovered -> JewelTheme.simpleListItemStyle.colors.backgroundActive
        else -> JewelTheme.simpleListItemStyle.colors.background
    }
    Box(
        modifier = Modifier.hoverable(interactionSource).pointerHoverIcon(PointerIcon.Hand)
            .background(backgroundColor)
            .clickable(
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
            StatusIndicator(request.status)
            Text(request.method, modifier = Modifier.width(60.dp))
            Text(request.statusCode?.toString() ?: "...", modifier = Modifier.width(50.dp))
            Text(request.uri, modifier = Modifier.weight(1f), maxLines = 1)
            Text(request.duration?.let { "${it}ms" } ?: "...", modifier = Modifier.width(100.dp))
        }
    }
}

@Composable
private fun KeyValueTable(data: Map<String, String>?) {
    if (data.isNullOrEmpty()) {
        Text("None")
        return
    }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        data.forEach { (key, value) ->
            Row {
                Text(key, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
                Text(value)
            }
        }
    }
}


@Composable
private fun BodyTab(title: String? = null, body: String?, project: Project) {
    Column(modifier = Modifier.fillMaxSize()) {
        if(title!=null){
            Text(title)
            Spacer(Modifier.height(8.dp))
        }
        body?.let { JsonTextPanel(it, project) }
    }
}


@Composable
private fun JsonTextPanel(text: String, project: Project) {
    val jsonPanel = JsonEditorTextPanel(project, text)
    println("JsonTextPanel composing with text length: ${text.length}")
    SwingPanel(
        factory = {
            jsonPanel.apply {
                isVisible = true
                minimumSize = Dimension(100, 100)
                preferredSize = Dimension(400, 300)
            }
        },
        update = { panel ->
            SwingUtilities.invokeLater {
                panel.text = text
                panel.revalidate()
                panel.repaint()
            }
        },
        modifier = Modifier.fillMaxSize().background(Color.Gray),
        background = JewelTheme.globalColors.panelBackground

    )
}
