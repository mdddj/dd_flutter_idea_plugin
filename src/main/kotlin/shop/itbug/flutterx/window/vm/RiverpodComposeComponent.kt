package shop.itbug.flutterx.window.vm

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
                RiverpodTopBar(riverpodState, scope)
                FrameStrip(state = riverpodState)
                HorizontalSplitLayout(
                    first = { ProviderListPanel(riverpodState) },
                    second = { ProviderDetailPanel(riverpodState, vmService, project) },
                    modifier = Modifier.fillMaxSize().weight(1f),
                    firstPaneMinWidth = 100.dp,
                    secondPaneMinWidth = 100.dp,
                )
            }
        }
    }
}

@Composable
private fun RiverpodTopBar(state: RiverpodState, scope: CoroutineScope) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconActionButton(
            AllIconsKeys.Actions.Refresh,
            contentDescription = "Refresh",
            onClick = { scope.launch { state.refresh() } }
        )

        Divider(Orientation.Vertical, Modifier.height(16.dp).width(1.dp))

        IconActionButton(
            AllIconsKeys.Actions.Play_first,
            contentDescription = "First Frame",
            enabled = state.selectedFrameIndex > 0,
            onClick = { state.selectFrame(0) }
        )

        IconActionButton(
            AllIconsKeys.General.ArrowLeft,
            contentDescription = "Previous Frame",
            enabled = state.selectedFrameIndex > 0,
            onClick = { state.navigateFrame(-1) }
        )

        Text(
            "Frame ${state.selectedFrameIndex}/${state.maxFrameIndex}",
            color = JewelTheme.globalColors.text.info
        )

        IconActionButton(
            AllIconsKeys.General.ArrowRight,
            contentDescription = "Next Frame",
            enabled = state.selectedFrameIndex < state.maxFrameIndex,
            onClick = { state.navigateFrame(1) }
        )

        IconActionButton(
            AllIconsKeys.Actions.Play_last,
            contentDescription = "Last Frame",
            enabled = state.selectedFrameIndex < state.maxFrameIndex,
            onClick = { state.selectFrame(state.maxFrameIndex) }
        )

        Spacer(Modifier.weight(1f))

        Text(
            "${state.providers.size} providers",
            color = JewelTheme.globalColors.text.info
        )
    }
}

/**
 * 帧步进器：像官方 devtool 一样，用圆点表示每一帧，
 * 并按当前选中 provider 在该帧的状态着色。
 */
@Composable
private fun FrameStrip(state: RiverpodState) {
    if (state.frames.isEmpty()) return
    val selectedElementId = state.selectedProvider?.elementId
    val frameCount = state.maxFrameIndex + 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JewelTheme.globalColors.panelBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("#", color = JewelTheme.globalColors.text.info)
        for (index in 0 until frameCount) {
            val isSelected = index == state.selectedFrameIndex
            val status = selectedElementId?.let { state.getStatusAt(it, index) }
            val dotColor = when (status) {
                RiverpodProviderStatus.Added,
                RiverpodProviderStatus.Modified -> Color(0xFF4EC9B0)
                RiverpodProviderStatus.Disposed -> Color(0xFFCE9178)
                null -> JewelTheme.globalColors.text.info.copy(alpha = 0.35f)
            }
            val borderColor = if (isSelected) {
                JewelTheme.globalColors.borders.focused
            } else {
                Color.Transparent
            }
            Box(
                modifier = Modifier
                    .size(if (isSelected) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .clickable { state.selectFrame(index) }
                    .pointerHoverIcon(PointerIcon.Hand)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "· tap a dot to inspect that frame",
            color = JewelTheme.globalColors.text.info
        )
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
                        it.containerHash.lowercase().contains(q)
            }
        }
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
            placeholder = { Text("Filter providers...") },
        )

        if (state.error != null) {
            Text(
                state.error ?: "",
                color = JewelTheme.globalColors.text.error
            )
        }

        if (filteredProviders.isEmpty() && state.providers.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching providers", color = JewelTheme.globalColors.text.info)
            }
        } else if (state.providers.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No provider events recorded. Interact with your app to trigger provider changes.",
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
                    item(key = "header_${section.label}") {
                        SectionHeader(section)
                    }
                    items(section.providers, key = { it.elementId }) { provider ->
                        ProviderRow(
                            provider = provider,
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
        ProviderSection("Modified", Color(0xFF4EC9B0), modified),
        ProviderSection("Disposed", Color(0xFFCE9178), disposed),
        ProviderSection("Unchanged", Color(0xFF808080), unchanged),
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
    status: RiverpodProviderStatus?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = if (selected) {
        JewelTheme.globalColors.panelBackground
    } else if (JewelTheme.isDark) {
        Color(0xFF2D2D30)
    } else {
        Color.White
    }
    val statusColor = when (status) {
        RiverpodProviderStatus.Added,
        RiverpodProviderStatus.Modified -> Color(0xFF4EC9B0)
        RiverpodProviderStatus.Disposed -> Color(0xFFCE9178)
        null -> JewelTheme.globalColors.text.info.copy(alpha = 0.4f)
    }
    val subtitle = if (provider.origin?.isFamily == true) {
        provider.arg
    } else {
        "scoped at #${provider.containerHash}"
    }

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
                Text(
                    subtitle.ifBlank { "—" },
                    maxLines = 1,
                    color = JewelTheme.globalColors.text.info,
                )
            }
            if (provider.hasState) {
                Text(
                    "●",
                    color = Color(0xFF569CD6),
                    modifier = Modifier.padding(end = 4.dp)
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
                    "Select a provider to inspect its state",
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
    val statusColor = when (provider.status) {
        RiverpodProviderStatus.Added,
        RiverpodProviderStatus.Modified -> Color(0xFF4EC9B0)
        RiverpodProviderStatus.Disposed -> Color(0xFFCE9178)
        null -> JewelTheme.globalColors.text.info.copy(alpha = 0.4f)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
            Text(provider.name.ifBlank { "Provider" }, fontWeight = FontWeight.Bold)
            Text(
                provider.lastEventType.toDisplayString(),
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                "frame #${provider.frameIndex}",
                color = JewelTheme.globalColors.text.info
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (provider.arg.isNotBlank()) {
                Text("arg: ${provider.arg}", color = JewelTheme.globalColors.text.info)
            }
            Text("container: #${provider.containerHash}", color = JewelTheme.globalColors.text.info)
            if (provider.origin?.isFamily == true) {
                Text("family", color = JewelTheme.globalColors.text.info)
            }
        }
        val elementState = state.getElementStateAt(provider.elementId, state.selectedFrameIndex)
        if (elementState != null && elementState.parents.isNotEmpty()) {
            Text(
                "dependencies: ${elementState.parents.size} · this provider listens to "
                        + "${elementState.parents.size} upstream provider(s)",
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("State", fontWeight = FontWeight.Bold)
            Text(
                "· state at frame #${state.selectedFrameIndex}",
                color = JewelTheme.globalColors.text.info
            )
        }
        if (statePath != null) {
            StateViewer(vmService, statePath)
        } else {
            Text("State not available for this provider", color = JewelTheme.globalColors.text.info)
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
        Text("Frame Events", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No events for this provider", color = JewelTheme.globalColors.text.info)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(events, key = { "${it.first}_${it.second.type}" }) { (frameIndex, event) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (frameIndex == state.selectedFrameIndex) {
                                        JewelTheme.globalColors.panelBackground
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "F$frameIndex",
                                color = JewelTheme.globalColors.text.info
                            )
                            Text(event.type.toDisplayString(), fontWeight = FontWeight.Medium)
                            if (event.hasState) {
                                Text("has state", color = Color(0xFF569CD6))
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
}
