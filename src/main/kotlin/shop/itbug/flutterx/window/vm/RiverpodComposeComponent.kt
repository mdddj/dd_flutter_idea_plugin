@file:OptIn(ExperimentalFoundationApi::class)

package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.services.PubspecService
import shop.itbug.flutterx.widget.CustomTabRow
import vm.devtool.*

@Composable
fun RiverpodComposeComponent(context: DartVmDevToolContext) {
    val project = context.project
    val pubspecService = PubspecService.getInstance(project)
    val hasRiverpodDeps = pubspecService.hasRiverpod()
    FlutterAppsTabComponent(context) {
        if (!hasRiverpodDeps) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("notfound_riverpod_deps"))
            }
        } else {
            RiverpodBody(it.vmService, project)
        }
    }
}

@Composable
private fun RiverpodBody(vmService: vm.VmService, project: Project) {
    val riverpodState = remember(vmService) { RiverpodState(vmService) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vmService) {
        riverpodState.init()
    }

    DisposableEffect(vmService) {
        onDispose {
            riverpodState.dispose()
        }
    }

    val available = riverpodState.isAvailable

    when {
        available == null || riverpodState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting to Riverpod DevTool...")
                }
            }
        }

        !available -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        key = AllIconsKeys.General.Warning,
                        contentDescription = "Not Available"
                    )
                    Text(
                        riverpodState.error ?: "Riverpod DevTool not available",
                        color = JewelTheme.globalColors.text.warning
                    )
                    OutlinedButton(
                        onClick = { riverpodState.init() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        true -> {
            Column(modifier = Modifier.fillMaxSize()) {
                RiverpodFrameBar(riverpodState, scope)
                HorizontalSplitLayout(
                    first = { ProviderListPanel(riverpodState) },
                    second = { ProviderDetailPanel(riverpodState, vmService, project) },
                    modifier = Modifier.fillMaxSize().weight(1f),
                    firstPaneMinWidth = 200.dp,
                    secondPaneMinWidth = 240.dp,
                )
            }
        }
    }
}

/**
 * 帧控制：上一帧/下一帧，加上带编号的帧按钮。
 * 编号从 1 开始，和内部 0 起的下标分开，避免 "4/4" 旁边却有 5 个点。
 */
@Composable
private fun RiverpodFrameBar(state: RiverpodState, scope: CoroutineScope) {
    val frameCount = state.maxFrameIndex + 1
    val selected = state.selectedFrameIndex
    val selectedElementId = state.selectedProvider?.elementId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconActionButton(
                AllIconsKeys.Actions.Refresh,
                contentDescription = PluginBundle.get("riverpod.refresh"),
                onClick = { scope.launch { state.refresh() } }
            )
            if (frameCount > 0) {
                IconActionButton(
                    AllIconsKeys.General.ArrowLeft,
                    contentDescription = PluginBundle.get("riverpod.frame.previous"),
                    enabled = selected > 0,
                    onClick = { state.navigateFrame(-1) }
                )
                Text(
                    PluginBundle.get("riverpod.frame.position", selected + 1, frameCount),
                    fontWeight = FontWeight.Medium
                )
                IconActionButton(
                    AllIconsKeys.General.ArrowRight,
                    contentDescription = PluginBundle.get("riverpod.frame.next"),
                    enabled = selected < state.maxFrameIndex,
                    onClick = { state.navigateFrame(1) }
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                PluginBundle.get("riverpod.providers.count", state.providers.size),
                color = JewelTheme.globalColors.text.info
            )
        }

        if (frameCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (index in 0 until frameCount) {
                    FrameChip(
                        index = index,
                        selected = index == selected,
                        status = selectedElementId?.let { state.getStatusAt(it, index) },
                        onClick = { state.selectFrame(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameChip(
    index: Int,
    selected: Boolean,
    status: RiverpodProviderStatus?,
    onClick: () -> Unit,
) {
    val statusColor = providerStatusColor(status)
    val background = when {
        selected -> statusColor.copy(alpha = 0.28f)
        status != null -> statusColor.copy(alpha = 0.16f)
        else -> Color.Transparent
    }
    val border = if (selected) {
        JewelTheme.globalColors.borders.focused
    } else if (status != null) {
        statusColor.copy(alpha = 0.55f)
    } else {
        JewelTheme.globalColors.borders.normal
    }
    Tooltip(tooltip = {
        Text(PluginBundle.get("riverpod.frame.chip", index + 1, statusLabel(status)))
    }) {
        Box(
            modifier = Modifier
                .height(22.dp)
                .widthIn(min = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(background)
                .border(1.dp, border, RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${index + 1}",
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ProviderListPanel(state: RiverpodState) {
    val searchState = rememberTextFieldState("")
    val query = searchState.text.toString().trim()

    val frameIndex = state.selectedFrameIndex

    val filteredProviders = remember(state.providers, query) {
        if (query.isBlank()) state.providers
        else {
            val q = query.lowercase()
            state.providers.filter {
                it.name.lowercase().contains(q) ||
                        it.arg.lowercase().contains(q) ||
                        it.containerHash.lowercase().contains(q) ||
                        it.elementId.lowercase().contains(q) ||
                        it.hashValue.lowercase().contains(q) ||
                        it.creationStackTrace?.lowercase()?.contains(q) == true
            }
        }
    }
    val duplicateNames = remember(filteredProviders) {
        filteredProviders.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    }
    val containerCount = remember(filteredProviders) {
        filteredProviders.map { it.containerId }.toSet().size
    }
    // 状态分组基于"当前选中帧"中 provider 的实际状态（与官方 devtool 一致）
    val sections = remember(filteredProviders, frameIndex, state.frames) {
        groupSections(filteredProviders) { state.getStatusAt(it.elementId, frameIndex) }
    }
    val listState = rememberLazyListState()
    val selectedElementId = state.selectedProvider?.elementId

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TextField(
            state = searchState,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(PluginBundle.get("riverpod.filter")) },
        )

        if (state.error != null) {
            Text(
                state.error ?: "",
                color = JewelTheme.globalColors.text.error
            )
        }

        if (filteredProviders.isEmpty() && state.providers.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("riverpod.list.no.match"), color = JewelTheme.globalColors.text.info)
            }
        } else if (state.providers.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    PluginBundle.get("riverpod.list.empty"),
                    color = JewelTheme.globalColors.text.info
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.id}") {
                        SectionHeader(section)
                    }
                    items(section.providers, key = { it.elementId }) { provider ->
                        ProviderRow(
                            provider = provider,
                            subtitle = providerSubtitle(
                                provider,
                                nameIsDuplicate = provider.name in duplicateNames,
                                containerCount = containerCount,
                            ),
                            status = state.getStatusAt(provider.elementId, frameIndex),
                            selected = provider.elementId == selectedElementId,
                            onSelect = { state.selectProvider(provider) }
                        )
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

private data class ProviderSection(
    val id: String,
    val label: String,
    val color: Color,
    val providers: List<RiverpodProviderInfo>
)

private fun groupSections(
    providers: List<RiverpodProviderInfo>,
    statusOf: (RiverpodProviderInfo) -> RiverpodProviderStatus?
): List<ProviderSection> {
    val modified = providers.filter {
        val s = statusOf(it)
        s == RiverpodProviderStatus.Added || s == RiverpodProviderStatus.Modified
    }
    val disposed = providers.filter { statusOf(it) == RiverpodProviderStatus.Disposed }
    val unchanged = providers.filter { statusOf(it) == null }
    return listOf(
        ProviderSection("modified", PluginBundle.get("riverpod.section.modified"), Color(0xFF4EC9B0), modified),
        ProviderSection("disposed", PluginBundle.get("riverpod.section.disposed"), Color(0xFFCE9178), disposed),
        ProviderSection("unchanged", PluginBundle.get("riverpod.section.unchanged"), Color(0xFF808080), unchanged),
    ).filter { it.providers.isNotEmpty() }
}

@Composable
private fun SectionHeader(section: ProviderSection) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(section.color)
        )
        Text(
            section.label,
            fontWeight = FontWeight.Bold,
            color = section.color
        )
        Text(
            "(${section.providers.size})",
            color = JewelTheme.globalColors.text.info
        )
    }
}

@Composable
private fun ProviderRow(
    provider: RiverpodProviderInfo,
    subtitle: String,
    status: RiverpodProviderStatus?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = if (selected) {
        JewelTheme.globalColors.outlines.focused.copy(alpha = 0.22f)
    } else if (JewelTheme.isDark) {
        Color(0xFF2D2D30)
    } else {
        Color.White
    }
    val statusColor = providerStatusColor(status)

    Tooltip(tooltip = {
        Column {
            Text(provider.name.ifBlank { "Provider" })
            Text(provider.elementId, color = JewelTheme.globalColors.text.info)
        }
    }) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .border(
                    width = if (selected) 1.dp else 0.dp,
                    color = if (selected) JewelTheme.globalColors.borders.focused else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onSelect)
                .pointerHoverIcon(PointerIcon.Hand)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        provider.name.ifBlank { "Provider" },
                        maxLines = 1,
                        fontWeight = FontWeight.Medium
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            maxLines = 1,
                            color = JewelTheme.globalColors.text.info,
                        )
                    }
                }
                Text(
                    statusLabel(status),
                    color = statusColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StateViewer(vmService: vm.VmService, statePath: String?) {
    var stateInstance by remember { mutableStateOf<vm.element.Instance?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(statePath) {
        isLoading = true
        error = null
        try {
            val eval = RiverpodHelper.getRiverpodEval(vmService)
            stateInstance = if (eval != null && statePath != null) {
                RiverpodHelper.getStateInstance(eval, vmService, statePath)
            } else {
                null
            }
        } catch (e: Exception) {
            error = e.message
        }
        isLoading = false
    }

    if (isLoading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Loading state...")
        }
    } else if (error != null) {
        Text("Error: $error", color = JewelTheme.globalColors.text.error)
    } else if (stateInstance == null || stateInstance!!.isNull()) {
        Text("null", color = Color(0xFF569CD6))
    } else {
        InstanceTreeView(vmService, stateInstance!!)
    }
}

@Composable
private fun InstanceTreeView(vmService: vm.VmService, instance: vm.element.Instance) {
    var isExpanded by remember { mutableStateOf(true) }
    val kind = instance.getKind()

    Column {
        when (kind) {
            vm.element.InstanceKind.String -> {
                Text(instance.getValueAsString() ?: "", color = Color(0xFFCE9178))
            }

            vm.element.InstanceKind.Int,
            vm.element.InstanceKind.Double -> {
                Text(instance.getValueAsString() ?: "0", color = Color(0xFFB5CEA8))
            }

            vm.element.InstanceKind.Bool -> {
                Text(instance.getValueAsString() ?: "false", color = Color(0xFF569CD6))
            }

            vm.element.InstanceKind.Null -> {
                Text("null", color = Color(0xFF569CD6))
            }

            vm.element.InstanceKind.List -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                        contentDescription = "Toggle",
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    )
                    Text("List(${instance.getLength()})", fontWeight = FontWeight.Bold)
                }
                if (isExpanded) {
                    val elements = instance.getElements() ?: emptyList()
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        elements.forEachIndexed { index, elementRef ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("[$index]: ", color = JewelTheme.globalColors.text.info)
                                DelayedInstanceViewer(vmService, elementRef)
                            }
                        }
                    }
                }
            }

            vm.element.InstanceKind.Map -> {
                val associations = instance.getAssociations()
                val assocList = associations?.toList() ?: emptyList()
                val size = assocList.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                        contentDescription = "Toggle",
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    )
                    Text("Map($size)", fontWeight = FontWeight.Bold)
                }
                if (isExpanded && assocList.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        assocList.forEach { assoc ->
                            Row(verticalAlignment = Alignment.Top) {
                                DelayedInstanceViewer(vmService, assoc.getKey())
                                Text(": ")
                                DelayedInstanceViewer(vmService, assoc.getValue())
                            }
                        }
                    }
                }
            }

            else -> {
                val classRef = instance.getClassRef()
                val fields = instance.getFields()
                val typeName = classRef.getName()
                val hash = instance.getIdentityHashCode()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        key = if (isExpanded) AllIconsKeys.General.ArrowDown else AllIconsKeys.General.ArrowRight,
                        contentDescription = "Toggle",
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    )
                    Text("$typeName #${hash.toString(16).take(4)}", color = Color(0xFF4EC9B0), fontWeight = FontWeight.Bold)
                }
                if (isExpanded && fields != null) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        fields
                            .filter { it.getDecl()?.isStatic() != true }
                            .sortedBy { it.getDecl()?.getName() }
                            .forEach { field ->
                                val fieldName = field.getDecl()?.getName() ?: "?"
                                val fieldRef = field.getValue()
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("$fieldName: ", fontWeight = FontWeight.Bold)
                                    if (fieldRef != null) {
                                        DelayedInstanceViewer(vmService, fieldRef)
                                    } else {
                                        Text("null", color = Color(0xFF569CD6))
                                    }
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun DelayedInstanceViewer(vmService: vm.VmService, ref: vm.element.InstanceRef?) {
    if (ref == null) {
        Text("null", color = Color(0xFF569CD6))
        return
    }
    var instance by remember { mutableStateOf<vm.element.Instance?>(null) }
    val eval = remember(vmService) { RiverpodHelper.getRiverpodEval(vmService) }

    LaunchedEffect(ref.getId()) {
        if (eval != null) {
            instance = try {
                eval.getInstance(vmService.getMainIsolateId(), ref)
            } catch (e: Exception) {
                null
            }
        }
    }

    when {
        instance != null -> InstanceTreeView(vmService, instance!!)
        else -> Text("...", color = JewelTheme.globalColors.text.info)
    }
}

@Composable
private fun ProviderDetailPanel(state: RiverpodState, vmService: vm.VmService, project: Project) {
    val provider = state.selectedProvider
    if (provider == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    key = AllIconsKeys.General.Beta,
                    contentDescription = "Select a provider"
                )
                Text(
                    PluginBundle.get("riverpod.select"),
                    color = JewelTheme.globalColors.text.info
                )
            }
        }
        return
    }

    var selectedTab by remember(provider.elementId) { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        ProviderHeader(state, provider)
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        CustomTabRow(
            selectedTabIndex = selectedTab,
            tabs = listOf(
                PluginBundle.get("riverpod.tab.state.diff"),
                PluginBundle.get("riverpod.tab.state.tree"),
                PluginBundle.get("riverpod.tab.events"),
            ),
            onTabClick = { selectedTab = it }
        )
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (selectedTab) {
                0 -> RiverpodStateDiffView(
                    project = project,
                    vmService = vmService,
                    state = state,
                    provider = provider,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> StateTreeTab(state, vmService, provider)
                else -> EventsTab(state, provider)
            }
        }
    }
}

@Composable
private fun ProviderHeader(state: RiverpodState, provider: RiverpodProviderInfo) {
    val frameStatus = state.getStatusAt(provider.elementId, state.selectedFrameIndex)
    val statusColor = providerStatusColor(frameStatus)
    val duplicate = state.providers.count { it.name == provider.name } > 1
    val containerCount = state.providers.map { it.containerId }.toSet().size
    val subtitle = providerSubtitle(provider, duplicate, containerCount)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
            Text(provider.name.ifBlank { "Provider" }, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
            Text(
                statusLabel(frameStatus),
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = JewelTheme.globalColors.text.info, maxLines = 1)
        }
        val elementState = state.getElementStateAt(provider.elementId, state.selectedFrameIndex)
        if (elementState != null && elementState.parents.isNotEmpty()) {
            Text(
                PluginBundle.get("riverpod.dependencies", elementState.parents.size),
                color = JewelTheme.globalColors.text.info
            )
        }
    }
}

@Composable
private fun StateTreeTab(state: RiverpodState, vmService: vm.VmService, provider: RiverpodProviderInfo) {
    val statePath = state.getCurrentStatePath(provider.elementId)
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            PluginBundle.get("riverpod.state.tree.caption", state.selectedFrameIndex + 1),
            fontWeight = FontWeight.Bold
        )
        if (statePath != null) {
            StateViewer(vmService, statePath)
        } else {
            Text(PluginBundle.get("riverpod.state.diff.no.state"), color = JewelTheme.globalColors.text.info)
        }
    }
}

@Composable
private fun EventsTab(state: RiverpodState, provider: RiverpodProviderInfo) {
    val events = state.frames.flatMap { frame ->
        frame.events
            .filter { it.providerMeta?.elementId == provider.elementId }
            .map { frame.index to it }
    }
    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("riverpod.events.empty"), color = JewelTheme.globalColors.text.info)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(events.size) { index ->
                        val (frameIndex, event) = events[index]
                        val selectedFrame = frameIndex == state.selectedFrameIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedFrame) {
                                        JewelTheme.globalColors.outlines.focused.copy(alpha = 0.18f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { state.selectFrame(frameIndex) }
                                .pointerHoverIcon(PointerIcon.Hand)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${frameIndex + 1}",
                                color = JewelTheme.globalColors.text.info,
                                fontWeight = if (selectedFrame) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(event.type.toDisplayString(), fontWeight = FontWeight.Medium)
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
}

private val stackLocation = Regex("""([\w./\\+-]+\.dart):(\d+)""")

private fun statusLabel(status: RiverpodProviderStatus?): String = when (status) {
    RiverpodProviderStatus.Added -> PluginBundle.get("riverpod.status.added")
    RiverpodProviderStatus.Modified -> PluginBundle.get("riverpod.status.modified")
    RiverpodProviderStatus.Disposed -> PluginBundle.get("riverpod.status.disposed")
    null -> PluginBundle.get("riverpod.status.unchanged")
}

private fun providerStatusColor(status: RiverpodProviderStatus?): Color = when (status) {
    RiverpodProviderStatus.Added,
    RiverpodProviderStatus.Modified -> Color(0xFF4EC9B0)
    RiverpodProviderStatus.Disposed -> Color(0xFFCE9178)
    null -> Color(0xFF808080)
}

/** 同名 Provider 用文件位置或短编号区分，不再显示大家都一样的 container hash。 */
private fun providerSubtitle(
    provider: RiverpodProviderInfo,
    nameIsDuplicate: Boolean,
    containerCount: Int,
): String {
    val arg = provider.arg.trim()
    val usefulArg = arg.isNotBlank() && !arg.equals("null", ignoreCase = true)
    val location = firstStackLocation(provider.creationStackTrace)
    val id = shortProviderId(provider)
    val base = when {
        provider.origin?.isFamily == true && usefulArg -> arg
        usefulArg -> arg
        location != null -> location
        containerCount > 1 -> "#${provider.containerHash}"
        else -> ""
    }
    return when {
        nameIsDuplicate && base.isNotBlank() -> "$base · $id"
        nameIsDuplicate -> id
        else -> base
    }
}

private fun shortProviderId(provider: RiverpodProviderInfo): String {
    val raw = provider.hashValue.ifBlank { provider.elementId }.filter { it.isLetterOrDigit() }
    val tail = raw.takeLast(6).ifBlank { provider.elementId.takeLast(6) }
    return "#$tail"
}

private fun firstStackLocation(stack: String?): String? {
    if (stack.isNullOrBlank()) return null
    val match = stackLocation.find(stack) ?: return null
    val file = match.groupValues[1].substringAfterLast('/').substringAfterLast('\\')
    return "$file:${match.groupValues[2]}"
}
