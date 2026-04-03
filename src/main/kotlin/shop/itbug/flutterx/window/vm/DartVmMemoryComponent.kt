package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.HorizontalSplitLayout
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.SplitLayoutState
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.simpleListItemStyle
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.document.copyTextToClipboard
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.util.toast
import shop.itbug.flutterx.util.toastWithError
import shop.itbug.flutterx.widget.CustomTabRow
import vm.memory.DartVmMemoryController
import vm.memory.HeapSnapshotRecord
import vm.memory.HeapSnapshotStatus
import vm.memory.MemoryChartPoint
import vm.memory.MemoryUsageSummary
import vm.memory.ProfileClassStat
import vm.memory.SnapshotClassDiff
import vm.memory.SnapshotDiffSummary
import vm.memory.TraceClassItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

private enum class DiffSortMode {
    AbsDeltaBytes,
    DeltaBytes,
    AbsDeltaInstances,
    DeltaInstances,
    AfterBytes,
    AddedObjects,
    RemovedObjects,
    ClassName,
}

private enum class IdentityViewMode {
    Both,
    AddedOnly,
    RemovedOnly,
}

private enum class MemoryChartRange(val labelKey: String, val windowMillis: Long?) {
    LastMinute("vm.memory.range.1m", 60_000L),
    LastThreeMinutes("vm.memory.range.3m", 180_000L),
    All("vm.memory.range.all", null),
}

@Composable
fun DartVmMemoryComponent(project: Project) {
    FlutterAppsTabComponent(project) {
        DartVmMemoryScreen(it, project)
    }
}

@Composable
private fun DartVmMemoryScreen(app: FlutterAppInstance, project: Project) {
    val scope = rememberCoroutineScope()
    val controller = remember(app.vmService) {
        DartVmMemoryController(app.vmService, scope)
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }

    val status by controller.statusMessage.collectAsState()
    val isolates by controller.isolates.collectAsState()
    val selectedIsolateId by controller.selectedIsolateId.collectAsState()

    val profileLoading by controller.profileLoading.collectAsState()
    val profileError by controller.profileError.collectAsState()
    val memoryUsage by controller.memoryUsage.collectAsState()
    val memoryChartPoints by controller.memoryChartPoints.collectAsState()
    val memoryGcEvents by controller.memoryGcEvents.collectAsState()
    val profileStats by controller.profileStats.collectAsState()

    val snapshotInProgress by controller.snapshotInProgress.collectAsState()
    val snapshots by controller.snapshots.collectAsState()
    val snapshotDiffSummary by controller.snapshotDiffSummary.collectAsState()
    val selectedDiffBeforeSnapshotId by controller.selectedDiffBeforeSnapshotId.collectAsState()
    val selectedDiffAfterSnapshotId by controller.selectedDiffAfterSnapshotId.collectAsState()

    val traceClassLoading by controller.traceClassLoading.collectAsState()
    val traceLoading by controller.traceLoading.collectAsState()
    val traceError by controller.traceError.collectAsState()
    val traceClasses by controller.traceClasses.collectAsState()
    val selectedTraceClassId by controller.selectedTraceClassId.collectAsState()
    val tracedClassIds by controller.tracedClassIds.collectAsState()
    val traceSampleCount by controller.traceSampleCount.collectAsState()
    val traceInstanceCount by controller.traceInstanceCount.collectAsState()
    val firstInstanceId by controller.firstInstanceId.collectAsState()
    val retainingPathLines by controller.retainingPathLines.collectAsState()

    var featureTabIndex by remember { mutableIntStateOf(0) }
    val featureTabs = listOf(
        PluginBundle.get("vm.memory.tab.profile"),
        PluginBundle.get("vm.memory.tab.diff"),
        PluginBundle.get("vm.memory.tab.trace"),
    )

    val isolateTabs = remember(isolates) {
        isolates.map { isolate ->
            val name = isolate.getName().orEmpty().ifBlank { PluginBundle.get("vm.memory.isolate.default") }
            val id = isolate.getId().orEmpty()
            "$name (${id.takeLast(6)})"
        }
    }
    val selectedIsolateIndex = remember(isolates, selectedIsolateId) {
        val index = isolates.indexOfFirst { it.getId() == selectedIsolateId }
        if (index < 0) 0 else index
    }
    val selectedTraceClass = remember(traceClasses, selectedTraceClassId) {
        traceClasses.firstOrNull { it.classId == selectedTraceClassId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, color = JewelTheme.globalColors.borders.normal)
            .background(JewelTheme.globalColors.panelBackground),
    ) {
        if (isolateTabs.isNotEmpty()) {
            CustomTabRow(
                selectedTabIndex = selectedIsolateIndex,
                tabs = isolateTabs,
                onTabClick = { index ->
                    isolates.getOrNull(index)?.getId()?.let(controller::selectIsolate)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        }

        CustomTabRow(
            selectedTabIndex = featureTabIndex,
            tabs = featureTabs,
            onTabClick = { featureTabIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (featureTabIndex) {
                0 -> MemoryProfileTab(
                    loading = profileLoading,
                    error = profileError,
                    usage = memoryUsage,
                    chartPoints = memoryChartPoints,
                    gcEvents = memoryGcEvents,
                    stats = profileStats,
                    onRefresh = { controller.refreshProfileAsync(gc = false) },
                    onGc = { controller.refreshProfileAsync(gc = true) },
                )

                1 -> MemoryDiffTab(
                    project = project,
                    snapshotInProgress = snapshotInProgress,
                    snapshots = snapshots,
                    diffSummary = snapshotDiffSummary,
                    selectedBeforeSnapshotId = selectedDiffBeforeSnapshotId,
                    selectedAfterSnapshotId = selectedDiffAfterSnapshotId,
                    onTakeSnapshot = { controller.requestHeapSnapshotAsync() },
                    onClearSnapshots = { controller.clearSnapshots() },
                    onSelectBeforeSnapshot = { controller.selectDiffBeforeSnapshot(it) },
                    onSelectAfterSnapshot = { controller.selectDiffAfterSnapshot(it) },
                )

                2 -> MemoryTraceTab(
                    loadingClasses = traceClassLoading,
                    loadingDetails = traceLoading,
                    error = traceError,
                    classes = traceClasses,
                    selectedClass = selectedTraceClass,
                    tracedClassIds = tracedClassIds,
                    sampleCount = traceSampleCount,
                    instanceCount = traceInstanceCount,
                    firstInstanceId = firstInstanceId,
                    retainingPathLines = retainingPathLines,
                    onRefreshClasses = { controller.refreshTraceClassesAsync() },
                    onSelectClass = { controller.selectTraceClass(it) },
                    onInspect = { controller.inspectSelectedTraceClassAsync() },
                    onEnableTrace = { classId -> controller.setClassTracingAsync(classId, true) },
                    onDisableTrace = { classId -> controller.setClassTracingAsync(classId, false) },
                )
            }
        }

        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
        Text(
            text = status ?: PluginBundle.get("vm.memory.status.ready"),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            color = JewelTheme.globalColors.text.info,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MemoryProfileTab(
    loading: Boolean,
    error: String?,
    usage: MemoryUsageSummary?,
    chartPoints: List<MemoryChartPoint>,
    gcEvents: List<Long>,
    stats: List<ProfileClassStat>,
    onRefresh: () -> Unit,
    onGc: () -> Unit,
) {
    val queryState = rememberTextFieldState("")
    val query = queryState.text.toString().trim()
    val filtered = remember(stats, query) {
        if (query.isBlank()) {
            stats
        } else {
            val q = query.lowercase()
            stats.filter {
                it.className.lowercase().contains(q) || it.libraryUri.lowercase().contains(q)
            }
        }
    }

    var chartRange by remember { mutableStateOf(MemoryChartRange.LastMinute) }
    var showAreaFill by remember { mutableStateOf(true) }
    var showHeapLine by remember { mutableStateOf(true) }
    var showExternalLine by remember { mutableStateOf(true) }
    var showCapacityLine by remember { mutableStateOf(true) }
    var showGcEvents by remember { mutableStateOf(true) }
    val visibleChartPoints = remember(chartPoints, chartRange) {
        filterChartPointsByRange(chartPoints, chartRange)
    }
    val visibleGcEvents = remember(gcEvents, visibleChartPoints) {
        val first = visibleChartPoints.firstOrNull()?.timestampMillis ?: return@remember emptyList()
        val last = visibleChartPoints.lastOrNull()?.timestampMillis ?: return@remember emptyList()
        gcEvents.filter { it in first..last }
    }

    val listState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(
                key = AllIconsKeys.Actions.Refresh,
                contentDescription = PluginBundle.get("vm.memory.profile.refresh.desc"),
                onClick = onRefresh,
            )
            OutlinedButton(onClick = onGc) {
                Text(PluginBundle.get("vm.memory.profile.gc"))
            }
            TextField(
                state = queryState,
                modifier = Modifier.weight(1f),
                placeholder = { Text(PluginBundle.get("vm.memory.profile.filter.placeholder")) },
            )
        }

        usage?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    PluginBundle.get(
                        "vm.memory.profile.heap.summary",
                        formatBytes(it.heapUsage),
                        formatBytes(it.heapCapacity),
                    ),
                )
                Text(PluginBundle.get("vm.memory.profile.external.summary", formatBytes(it.externalUsage)))
                Text(PluginBundle.get("vm.memory.profile.classes.summary", stats.size))
                Spacer(Modifier.width(10.dp))
                Text(PluginBundle.get("vm.memory.profile.range"), color = JewelTheme.globalColors.text.info)
                MemoryChartRange.entries.forEach { range ->
                    OutlinedButton(onClick = { chartRange = range }) {
                        val label = PluginBundle.get(range.labelKey)
                        Text(if (chartRange == range) "● $label" else label)
                    }
                }
                Spacer(Modifier.width(4.dp))
                MemoryLegendChip(
                    label = PluginBundle.get("vm.memory.profile.legend.heap"),
                    color = Color(0xFF34AFDE),
                    enabled = showHeapLine,
                    onClick = { showHeapLine = !showHeapLine },
                    dashPattern = floatArrayOf(6f, 4f),
                )
                MemoryLegendChip(
                    label = PluginBundle.get("vm.memory.profile.legend.external"),
                    color = Color(0xFFEF5350),
                    enabled = showExternalLine,
                    onClick = { showExternalLine = !showExternalLine },
                    dashPattern = floatArrayOf(8f, 6f),
                )
                MemoryLegendChip(
                    label = PluginBundle.get("vm.memory.profile.legend.capacity"),
                    color = Color(0xFFE57900),
                    enabled = showCapacityLine,
                    onClick = { showCapacityLine = !showCapacityLine },
                    dashPattern = floatArrayOf(8f, 6f),
                )
                MemoryLegendChip(
                    label = PluginBundle.get("vm.memory.profile.legend.area"),
                    color = Color(0xFF48BFEA),
                    enabled = showAreaFill,
                    onClick = { showAreaFill = !showAreaFill },
                    areaSample = true,
                )
                MemoryLegendChip(
                    label = PluginBundle.get("vm.memory.profile.legend.gc"),
                    color = Color(0xFF888888),
                    enabled = showGcEvents,
                    onClick = { showGcEvents = !showGcEvents },
                    verticalSample = true,
                )
            }
            Spacer(Modifier.height(6.dp))
            MemoryChartCard(
                points = visibleChartPoints,
                gcEvents = visibleGcEvents,
                range = chartRange,
                showAreaFill = showAreaFill,
                showHeapLine = showHeapLine,
                showExternalLine = showExternalLine,
                showCapacityLine = showCapacityLine,
                showGcEvents = showGcEvents,
                modifier = Modifier.fillMaxWidth().height(172.dp).padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(6.dp))
        }

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        PluginBundle.get("vm.memory.common.error", error),
                        color = JewelTheme.globalColors.text.error,
                    )
                }
            }

            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(PluginBundle.get("vm.memory.table.class"), modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text(PluginBundle.get("vm.memory.table.library"), modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text(PluginBundle.get("vm.memory.table.instances"), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(PluginBundle.get("vm.memory.table.bytes"), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        items(filtered.size) { index ->
                            val item = filtered[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = item.className,
                                    modifier = Modifier.weight(2f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.libraryUri.ifBlank { "-" },
                                    modifier = Modifier.weight(2f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = JewelTheme.globalColors.text.info,
                                )
                                Text(item.instancesCurrent.toString(), modifier = Modifier.weight(1f))
                                Text(formatBytes(item.bytesCurrent), modifier = Modifier.weight(1f))
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
}

@Composable
private fun MemoryLegendChip(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    dashPattern: FloatArray? = null,
    areaSample: Boolean = false,
    verticalSample: Boolean = false,
) {
    Row(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = if (enabled) 1f else 0.45f))
            .background(if (enabled) color.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.width(18.dp).height(9.dp)) {
            val sampleColor = if (enabled) color else color.copy(alpha = 0.4f)
            if (areaSample) {
                drawRect(sampleColor.copy(alpha = if (enabled) 0.26f else 0.14f))
            }
            if (verticalSample) {
                val x = size.width / 2f
                drawLine(
                    color = sampleColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.6f,
                )
            } else {
                drawLine(
                    color = sampleColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.8f,
                    pathEffect = dashPattern?.let { PathEffect.dashPathEffect(it) },
                )
            }
        }
        Text(
            text = if (enabled) label else PluginBundle.get("vm.memory.legend.off", label),
            color = if (enabled) color else JewelTheme.globalColors.text.info,
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun MemoryChartCard(
    points: List<MemoryChartPoint>,
    gcEvents: List<Long>,
    range: MemoryChartRange,
    showAreaFill: Boolean,
    showHeapLine: Boolean,
    showExternalLine: Boolean,
    showCapacityLine: Boolean,
    showGcEvents: Boolean,
    modifier: Modifier = Modifier,
) {
    val axisColor = Color(0xFF7F7F7F)
    val heapFillColor = Color(0x8048BFEA)
    val heapLineColor = Color(0xFF34AFDE)
    val capacityLineColor = Color(0xFFE57900)
    val externalLineColor = Color(0xFFEF5350)
    val gcColor = Color(0x3F888888)
    val mb = 1024L * 1024L
    val maxSeriesBytes = points.maxOfOrNull { point ->
        max(point.heapCapacity, max(point.heapUsage, point.externalUsage))
    } ?: 0L
    val axisStepBytes = 100L * mb
    val roundedMax = ((maxSeriesBytes + axisStepBytes - 1L) / axisStepBytes) * axisStepBytes
    val axisMaxBytes = max(roundedMax, 700L * mb).coerceAtLeast(axisStepBytes)
    val axisTicks = buildMemoryAxisTicks(axisMaxBytes, mb)
    val hasSeries = points.size >= 2
    var chartWidthPx by remember(points) { mutableIntStateOf(0) }
    var hoveredIndex by remember(points) { mutableStateOf<Int?>(null) }
    var hoveredPointerX by remember(points) { mutableStateOf<Float?>(null) }
    var tooltipWidthPx by remember { mutableIntStateOf(0) }

    val domainEndTs = points.lastOrNull()?.timestampMillis ?: 0L
    val domainStartTs = when (val window = range.windowMillis) {
        null -> points.firstOrNull()?.timestampMillis ?: 0L
        else -> domainEndTs - window
    }
    val domainSpan = max(1f, (domainEndTs - domainStartTs).toFloat())
    val hoveredPoint = hoveredIndex?.let { points.getOrNull(it) }
    LaunchedEffect(points.size, hoveredIndex) {
        val index = hoveredIndex ?: return@LaunchedEffect
        if (index >= points.size) {
            hoveredIndex = points.lastIndex.takeIf { it >= 0 }
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 4.dp),
    ) {
        Box(modifier = Modifier.width(68.dp).fillMaxHeight()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val axisX = size.width - 1f
                drawLine(
                    color = axisColor,
                    start = Offset(axisX, 0f),
                    end = Offset(axisX, size.height),
                    strokeWidth = 2.4f,
                )
                axisTicks.forEach { tick ->
                    val ratio = (tick.valueBytes.toFloat() / axisMaxBytes.toFloat()).coerceIn(0f, 1f)
                    val y = size.height - (size.height * ratio)
                    drawLine(
                        color = axisColor,
                        start = Offset(axisX - 8f, y),
                        end = Offset(axisX, y),
                        strokeWidth = 2.1f,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                axisTicks.forEach { tick ->
                    Text(tick.label, color = axisColor)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 6.dp)
                .onSizeChanged { chartWidthPx = it.width }
                .onPointerEvent(PointerEventType.Enter) { event ->
                    val x = event.changes.firstOrNull()?.position?.x ?: return@onPointerEvent
                    hoveredPointerX = x
                    hoveredIndex = nearestMemoryHoverIndex(points, domainStartTs, domainSpan, chartWidthPx, x)
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val x = event.changes.firstOrNull()?.position?.x ?: return@onPointerEvent
                    hoveredPointerX = x
                    hoveredIndex = nearestMemoryHoverIndex(points, domainStartTs, domainSpan, chartWidthPx, x)
                }
                .onPointerEvent(PointerEventType.Exit) {
                    hoveredPointerX = null
                    hoveredIndex = null
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val top = 2f
                val bottom = size.height - 1f
                val left = 0f
                val right = size.width
                drawLine(
                    color = axisColor,
                    start = Offset(left, bottom),
                    end = Offset(right, bottom),
                    strokeWidth = 2.4f,
                )
                if (!hasSeries) return@Canvas

                fun mapY(value: Long): Float {
                    val ratio = (value.toFloat() / axisMaxBytes.toFloat()).coerceIn(0f, 1f)
                    return bottom - ((bottom - top) * ratio)
                }

                fun mapX(timestampMillis: Long): Float {
                    val normalized = ((timestampMillis - domainStartTs).toFloat() / domainSpan).coerceIn(0f, 1f)
                    return normalized * size.width
                }

                fun buildPath(valueOf: (MemoryChartPoint) -> Long): Path {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = mapX(point.timestampMillis)
                        val y = mapY(valueOf(point))
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    return path
                }

                val heapPath = buildPath { it.heapUsage }
                val externalPath = buildPath { it.externalUsage }
                val capacityPath = buildPath { it.heapCapacity }

                if (showAreaFill) {
                    val areaPath = Path().apply {
                        val firstX = mapX(points.first().timestampMillis)
                        moveTo(firstX, bottom)
                        points.forEach { point ->
                            lineTo(mapX(point.timestampMillis), mapY(point.heapUsage))
                        }
                        lineTo(mapX(points.last().timestampMillis), bottom)
                        close()
                    }
                    drawPath(areaPath, color = heapFillColor)
                }

                if (showGcEvents) {
                    gcEvents.forEach { ts ->
                        val x = mapX(ts)
                        drawLine(
                            color = gcColor,
                            start = Offset(x, top),
                            end = Offset(x, bottom),
                            strokeWidth = 1f,
                        )
                    }
                }

                if (showCapacityLine) {
                    drawPath(
                        path = capacityPath,
                        color = capacityLineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        ),
                    )
                }
                if (showExternalLine) {
                    drawPath(
                        path = externalPath,
                        color = externalLineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.9f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        ),
                    )
                }
                if (showHeapLine) {
                    drawPath(
                        path = heapPath,
                        color = heapLineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.8f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                        ),
                    )
                }

                hoveredPoint?.let { point ->
                    val x = mapX(point.timestampMillis)
                    val y = mapY(point.heapUsage)
                    drawLine(
                        color = axisColor.copy(alpha = 0.85f),
                        start = Offset(x, top),
                        end = Offset(x, bottom),
                        strokeWidth = 1.1f,
                    )
                    drawCircle(
                        color = heapLineColor,
                        radius = 3.2f,
                        center = Offset(x, y),
                    )
                }
            }

            hoveredPoint?.let { point ->
                val tooltipX = run {
                    val pointer = hoveredPointerX ?: 0f
                    val raw = (pointer + 10f).roundToInt()
                    val upperBound = max(0, chartWidthPx - tooltipWidthPx - 4)
                    raw.coerceIn(0, upperBound)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(tooltipX, 0) }
                        .onSizeChanged { tooltipWidthPx = it.width }
                        .background(Color(0xDCE3E3E3))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        PluginBundle.get(
                            "vm.memory.chart.tooltip",
                            formatChartSampleTime(point.timestampMillis),
                            formatBytes(point.heapUsage),
                            formatBytes(point.externalUsage),
                            formatBytes(point.heapCapacity),
                        ),
                        color = Color(0xFF565656),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class MemoryAxisTick(
    val valueBytes: Long,
    val label: String,
)

private fun buildMemoryAxisTicks(axisMaxBytes: Long, mb: Long): List<MemoryAxisTick> {
    val axis700 = 700L * mb
    if (axisMaxBytes == axis700) {
        // Match DevTools screenshot style: keep the 300M slot but hide its text.
        return listOf(700L, 600L, 500L, 400L, 300L, 200L, 100L, 0L).map { v ->
            MemoryAxisTick(
                valueBytes = v * mb,
                label = when (v) {
                    0L -> "0"
                    300L -> ""
                    else -> "${v}M"
                },
            )
        }
    }
    val step = 100L * mb
    val top = (axisMaxBytes / step).coerceAtLeast(1L)
    return (top downTo 0L).map { unit ->
        val value = unit * step
        MemoryAxisTick(
            valueBytes = value,
            label = if (value <= 0L) "0" else "${value / mb}M",
        )
    }
}

private fun filterChartPointsByRange(
    points: List<MemoryChartPoint>,
    range: MemoryChartRange,
): List<MemoryChartPoint> {
    val window = range.windowMillis ?: return points
    if (points.isEmpty()) return points
    val cutoff = points.last().timestampMillis - window
    val filtered = points.filter { it.timestampMillis >= cutoff }
    if (filtered.size >= 2 || points.size <= 2) return filtered
    return points.takeLast(2)
}

private fun nearestMemoryHoverIndex(
    points: List<MemoryChartPoint>,
    domainStartTs: Long,
    domainSpan: Float,
    chartWidthPx: Int,
    pointerX: Float,
): Int? {
    if (points.isEmpty() || chartWidthPx <= 0 || domainSpan <= 0f) return null
    if (pointerX < 0f || pointerX > chartWidthPx.toFloat()) return null

    val normalizedX = (pointerX / chartWidthPx.toFloat()).coerceIn(0f, 1f)
    val firstNormalized = ((points.first().timestampMillis - domainStartTs).toFloat() / domainSpan).coerceIn(0f, 1f)
    val lastNormalized = ((points.last().timestampMillis - domainStartTs).toFloat() / domainSpan).coerceIn(0f, 1f)
    if (normalizedX < firstNormalized || normalizedX > lastNormalized) return null

    val targetTimestamp = domainStartTs + (normalizedX * domainSpan).toLong()
    var nearestIndex = 0
    var nearestDistance = Long.MAX_VALUE
    points.forEachIndexed { index, point ->
        val distance = abs(point.timestampMillis - targetTimestamp)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearestIndex = index
        }
    }
    return nearestIndex
}

@Composable
private fun MemoryDiffTab(
    project: Project,
    snapshotInProgress: Boolean,
    snapshots: List<HeapSnapshotRecord>,
    diffSummary: SnapshotDiffSummary?,
    selectedBeforeSnapshotId: Int?,
    selectedAfterSnapshotId: Int?,
    onTakeSnapshot: () -> Unit,
    onClearSnapshots: () -> Unit,
    onSelectBeforeSnapshot: (Int) -> Unit,
    onSelectAfterSnapshot: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val diffState = rememberLazyListState()
    val queryState = rememberTextFieldState("")
    var sortMode by remember { mutableStateOf(DiffSortMode.AbsDeltaBytes) }
    var sortDescending by remember { mutableStateOf(true) }

    val completedSnapshots = remember(snapshots) {
        snapshots
            .filter { it.status == HeapSnapshotStatus.Completed && it.parsedClassCount > 0 }
            .sortedBy { it.id }
    }
    val completedSnapshotTabs = remember(completedSnapshots) {
        completedSnapshots.map { "#${it.id}" }
    }
    val beforeIndex = remember(completedSnapshots, selectedBeforeSnapshotId) {
        val idx = completedSnapshots.indexOfFirst { it.id == selectedBeforeSnapshotId }
        if (idx >= 0) idx else maxOf(0, completedSnapshots.lastIndex - 1)
    }
    val afterIndex = remember(completedSnapshots, selectedAfterSnapshotId) {
        val idx = completedSnapshots.indexOfFirst { it.id == selectedAfterSnapshotId }
        if (idx >= 0) idx else completedSnapshots.lastIndex
    }

    val query = queryState.text.toString().trim()
    val filteredRows = remember(diffSummary, query) {
        val rows = diffSummary?.classDiffs ?: emptyList()
        if (query.isBlank()) {
            rows
        } else {
            val q = query.lowercase()
            rows.filter {
                it.className.lowercase().contains(q) ||
                    it.libraryUri.lowercase().contains(q) ||
                    it.classId.lowercase().contains(q)
            }
        }
    }
    val diffRows = remember(filteredRows, sortMode, sortDescending) {
        filteredRows.sortedWith(snapshotDiffComparator(sortMode, sortDescending))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onTakeSnapshot, enabled = !snapshotInProgress) {
                Text(
                    if (snapshotInProgress) PluginBundle.get("vm.memory.snapshot.collecting")
                    else PluginBundle.get("vm.memory.snapshot.take"),
                )
            }
            OutlinedButton(onClick = onClearSnapshots, enabled = snapshots.isNotEmpty()) {
                Text(PluginBundle.get("vm.memory.snapshot.clear"))
            }
            if (snapshotInProgress) {
                CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
            }
        }

        if (completedSnapshots.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(PluginBundle.get("vm.memory.snapshot.before"), fontWeight = FontWeight.Bold)
                CustomTabRow(
                    selectedTabIndex = beforeIndex,
                    tabs = completedSnapshotTabs,
                    onTabClick = { index ->
                        completedSnapshots.getOrNull(index)?.id?.let(onSelectBeforeSnapshot)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(PluginBundle.get("vm.memory.snapshot.after"), fontWeight = FontWeight.Bold)
                CustomTabRow(
                    selectedTabIndex = afterIndex,
                    tabs = completedSnapshotTabs,
                    onTabClick = { index ->
                        completedSnapshots.getOrNull(index)?.id?.let(onSelectAfterSnapshot)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        if (snapshots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("vm.memory.snapshot.empty"), color = JewelTheme.globalColors.text.info)
            }
        } else {
            var splitState by remember { mutableStateOf(SplitLayoutState(0.35f)) }
            HorizontalSplitLayout(
                modifier = Modifier.fillMaxSize(),
                state = splitState,
                firstPaneMinWidth = 220.dp,
                secondPaneMinWidth = 280.dp,
                first = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                            items(snapshots.size) { index ->
                                val item = snapshots[index]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .border(1.dp, JewelTheme.globalColors.borders.normal)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        PluginBundle.get("vm.memory.snapshot.card.title", item.id, item.status.name),
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        PluginBundle.get("vm.memory.snapshot.card.time", formatTime(item.startedAtMillis)),
                                        color = JewelTheme.globalColors.text.info,
                                    )
                                    Text(
                                        PluginBundle.get(
                                            "vm.memory.snapshot.card.chunks",
                                            item.chunkCount,
                                            formatBytes(item.payloadBytes),
                                        ),
                                    )
                                    Text(
                                        PluginBundle.get(
                                            "vm.memory.snapshot.card.heap.parsed",
                                            item.heapParsed,
                                            item.parsedClassCount,
                                        ),
                                    )
                                    Text(
                                        PluginBundle.get("vm.memory.snapshot.card.isolate", item.isolateId),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (!item.error.isNullOrBlank()) {
                                        Text(
                                            PluginBundle.get("vm.memory.common.error", item.error),
                                            color = JewelTheme.globalColors.text.error,
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
                },
                second = {
                    SnapshotDiffPanel(
                        project = project,
                        modifier = Modifier.fillMaxSize(),
                        diffSummary = diffSummary,
                        queryState = queryState,
                        rows = diffRows,
                        diffState = diffState,
                        sortMode = sortMode,
                        sortDescending = sortDescending,
                        onSortModeChange = { sortMode = it },
                        onToggleSortDirection = { sortDescending = !sortDescending },
                    )
                },
            )
        }
    }
}

@Composable
private fun SnapshotDiffPanel(
    project: Project,
    modifier: Modifier,
    diffSummary: SnapshotDiffSummary?,
    queryState: androidx.compose.foundation.text.input.TextFieldState,
    rows: List<SnapshotClassDiff>,
    diffState: androidx.compose.foundation.lazy.LazyListState,
    sortMode: DiffSortMode,
    sortDescending: Boolean,
    onSortModeChange: (DiffSortMode) -> Unit,
    onToggleSortDirection: () -> Unit,
) {
    var selectedClassId by remember(diffSummary?.beforeSnapshotId, diffSummary?.afterSnapshotId) {
        mutableStateOf<String?>(null)
    }
    Column(modifier = modifier.padding(top = 6.dp)) {
        if (diffSummary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    PluginBundle.get("vm.memory.diff.need.snapshots"),
                    color = JewelTheme.globalColors.text.info,
                )
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                PluginBundle.get("vm.memory.diff.header", diffSummary.beforeSnapshotId, diffSummary.afterSnapshotId),
                fontWeight = FontWeight.Bold,
            )
            Text(
                PluginBundle.get(
                    "vm.memory.diff.total.bytes",
                    formatBytes(diffSummary.totalBeforeBytes),
                    formatBytes(diffSummary.totalAfterBytes),
                    formatSignedBytes(diffSummary.totalDeltaBytes),
                ),
            )
            Text(
                PluginBundle.get("vm.memory.diff.total.instances.delta", formatSignedLong(diffSummary.totalDeltaInstances)),
                color = JewelTheme.globalColors.text.info,
            )
            Text(
                PluginBundle.get("vm.memory.diff.sources", diffSummary.beforeSource, diffSummary.afterSource),
                color = JewelTheme.globalColors.text.info,
            )
            if (diffSummary.identityBased) {
                Text(
                    PluginBundle.get(
                        "vm.memory.diff.identity.objects",
                        diffSummary.totalIdentityTrackedBefore,
                        diffSummary.totalIdentityTrackedAfter,
                        diffSummary.totalIdentityAdded,
                        diffSummary.totalIdentityRemoved,
                    ),
                    color = JewelTheme.globalColors.text.info,
                )
            } else {
                Text(
                    PluginBundle.get("vm.memory.diff.identity.unavailable"),
                    color = JewelTheme.globalColors.text.info,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = {
                    onSortModeChange(
                        when (sortMode) {
                            DiffSortMode.AbsDeltaBytes -> DiffSortMode.DeltaBytes
                            DiffSortMode.DeltaBytes -> DiffSortMode.AbsDeltaInstances
                            DiffSortMode.AbsDeltaInstances -> DiffSortMode.DeltaInstances
                            DiffSortMode.DeltaInstances -> DiffSortMode.AfterBytes
                            DiffSortMode.AfterBytes -> DiffSortMode.AddedObjects
                            DiffSortMode.AddedObjects -> DiffSortMode.RemovedObjects
                            DiffSortMode.RemovedObjects -> DiffSortMode.ClassName
                            DiffSortMode.ClassName -> DiffSortMode.AbsDeltaBytes
                        },
                    )
                }) {
                    Text(PluginBundle.get("vm.memory.diff.sort.button", sortModeLabel(sortMode)))
                }
                OutlinedButton(onClick = onToggleSortDirection) {
                    Text(
                        if (sortDescending) PluginBundle.get("vm.memory.diff.sort.desc")
                        else PluginBundle.get("vm.memory.diff.sort.asc"),
                    )
                }
            }
            TextField(
                state = queryState,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(PluginBundle.get("vm.memory.diff.filter.placeholder")) },
            )
        }
        Spacer(Modifier.height(6.dp))
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("vm.memory.diff.empty"), color = JewelTheme.globalColors.text.info)
            }
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(PluginBundle.get("vm.memory.table.class"), modifier = Modifier.weight(2.0f), fontWeight = FontWeight.Bold)
            Text(PluginBundle.get("vm.memory.table.library"), modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold)
            Text(PluginBundle.get("vm.memory.diff.table.delta.instances"), modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold)
            Text(PluginBundle.get("vm.memory.diff.table.delta.bytes"), modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold)
            Text(PluginBundle.get("vm.memory.diff.table.added.obj"), modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
            Text(PluginBundle.get("vm.memory.diff.table.removed.obj"), modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
            Text(PluginBundle.get("vm.memory.diff.table.after.bytes"), modifier = Modifier.weight(0.9f), fontWeight = FontWeight.Bold)
        }
        Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

        val selectedRow = remember(rows, selectedClassId) {
            rows.firstOrNull { it.classId == selectedClassId } ?: rows.firstOrNull()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.64f).fillMaxWidth()) {
                LazyColumn(modifier = Modifier.fillMaxSize(), state = diffState) {
                    items(rows.size) { index ->
                        val row = rows[index]
                        val identityAvailable = diffSummary.identityBased
                        val selected = selectedRow?.classId == row.classId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) JewelTheme.simpleListItemStyle.colors.backgroundSelectedActive
                                    else JewelTheme.globalColors.panelBackground,
                                )
                                .clickable { selectedClassId = row.classId }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                row.className,
                                modifier = Modifier.weight(2.0f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                row.libraryUri.ifBlank { "-" },
                                modifier = Modifier.weight(1.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = JewelTheme.globalColors.text.info,
                            )
                            Text(
                                formatSignedLong(row.deltaInstances),
                                modifier = Modifier.weight(0.9f),
                                color = deltaColor(row.deltaInstances),
                            )
                            Text(
                                formatSignedBytes(row.deltaBytes),
                                modifier = Modifier.weight(0.9f),
                                color = deltaColor(row.deltaBytes),
                            )
                            Text(
                                if (identityAvailable) row.identityAdded.toString() else PluginBundle.get("vm.memory.common.empty"),
                                modifier = Modifier.weight(0.7f),
                                color = if (identityAvailable) JewelTheme.globalColors.text.error else JewelTheme.globalColors.text.info,
                            )
                            Text(
                                if (identityAvailable) row.identityRemoved.toString() else PluginBundle.get("vm.memory.common.empty"),
                                modifier = Modifier.weight(0.7f),
                                color = JewelTheme.globalColors.text.info,
                            )
                            Text(
                                formatBytes(row.afterBytes),
                                modifier = Modifier.weight(0.9f),
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(diffState),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                )
            }

            Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

            SnapshotIdentityDetails(
                project = project,
                modifier = Modifier.weight(0.36f).fillMaxWidth(),
                row = selectedRow,
                identityBased = diffSummary.identityBased,
                beforeSnapshotId = diffSummary.beforeSnapshotId,
                afterSnapshotId = diffSummary.afterSnapshotId,
            )
        }
    }
}

@Composable
private fun SnapshotIdentityDetails(
    project: Project,
    modifier: Modifier,
    row: SnapshotClassDiff?,
    identityBased: Boolean,
    beforeSnapshotId: Int,
    afterSnapshotId: Int,
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (row == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("vm.memory.diff.select.class"), color = JewelTheme.globalColors.text.info)
            }
            return@Column
        }

        Text(
            PluginBundle.get("vm.memory.diff.object.title", row.className),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            row.libraryUri.ifBlank { "-" },
            color = JewelTheme.globalColors.text.info,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!identityBased) {
            Text(
                PluginBundle.get("vm.memory.diff.identity.hash.unavailable"),
                color = JewelTheme.globalColors.text.info,
            )
            return@Column
        }

        var viewMode by remember(row.classId, beforeSnapshotId, afterSnapshotId) {
            mutableStateOf(IdentityViewMode.Both)
        }
        val queryState = rememberTextFieldState("")
        val rawQuery = queryState.text.toString()
        var debouncedQuery by remember(row.classId, beforeSnapshotId, afterSnapshotId) {
            mutableStateOf("")
        }
        LaunchedEffect(rawQuery, row.classId, beforeSnapshotId, afterSnapshotId) {
            delay(180)
            debouncedQuery = rawQuery.trim()
        }
        val filteringInProgress = rawQuery.trim() != debouncedQuery
        val filteredAdded = remember(row.addedIdentityHashes, debouncedQuery) {
            filterIdentityHashes(row.addedIdentityHashes, debouncedQuery)
        }
        val filteredRemoved = remember(row.removedIdentityHashes, debouncedQuery) {
            filterIdentityHashes(row.removedIdentityHashes, debouncedQuery)
        }
        val visibleAdded = if (viewMode == IdentityViewMode.RemovedOnly) emptyList() else filteredAdded
        val visibleRemoved = if (viewMode == IdentityViewMode.AddedOnly) emptyList() else filteredRemoved

        Text(PluginBundle.get("vm.memory.diff.tracked", row.identityTrackedBefore, row.identityTrackedAfter))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { viewMode = IdentityViewMode.Both },
            ) {
                val text = PluginBundle.get("vm.memory.diff.view.both")
                Text(if (viewMode == IdentityViewMode.Both) PluginBundle.get("vm.memory.diff.view.prefix", text) else text)
            }
            OutlinedButton(
                onClick = { viewMode = IdentityViewMode.AddedOnly },
            ) {
                val text = PluginBundle.get("vm.memory.diff.view.added")
                Text(if (viewMode == IdentityViewMode.AddedOnly) PluginBundle.get("vm.memory.diff.view.prefix", text) else text)
            }
            OutlinedButton(
                onClick = { viewMode = IdentityViewMode.RemovedOnly },
            ) {
                val text = PluginBundle.get("vm.memory.diff.view.removed")
                Text(if (viewMode == IdentityViewMode.RemovedOnly) PluginBundle.get("vm.memory.diff.view.prefix", text) else text)
            }
            TextField(
                state = queryState,
                modifier = Modifier.weight(1f),
                placeholder = { Text(PluginBundle.get("vm.memory.diff.filter.hash.placeholder")) },
            )
        }
        Text(
            if (debouncedQuery.isBlank()) {
                PluginBundle.get("vm.memory.diff.visible.summary", visibleAdded.size, visibleRemoved.size)
            } else {
                PluginBundle.get("vm.memory.diff.visible.summary.filtered", visibleAdded.size, visibleRemoved.size)
            },
            color = JewelTheme.globalColors.text.info,
        )
        if (filteringInProgress) {
            Text(
                PluginBundle.get("vm.memory.diff.filter.applying"),
                color = JewelTheme.globalColors.text.info,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = {
                    buildIdentityClipboardText(
                        addedIdentityHashes = visibleAdded,
                        removedIdentityHashes = visibleRemoved,
                        includeAdded = true,
                        includeRemoved = false,
                    ).copyTextToClipboard()
                    project.toast(PluginBundle.get("vm.memory.diff.copy.added.toast", visibleAdded.size))
                },
                enabled = visibleAdded.isNotEmpty(),
            ) {
                Text(PluginBundle.get("vm.memory.diff.copy.added"))
            }
            OutlinedButton(
                onClick = {
                    buildIdentityClipboardText(
                        addedIdentityHashes = visibleAdded,
                        removedIdentityHashes = visibleRemoved,
                        includeAdded = false,
                        includeRemoved = true,
                    ).copyTextToClipboard()
                    project.toast(PluginBundle.get("vm.memory.diff.copy.removed.toast", visibleRemoved.size))
                },
                enabled = visibleRemoved.isNotEmpty(),
            ) {
                Text(PluginBundle.get("vm.memory.diff.copy.removed"))
            }
            OutlinedButton(
                onClick = {
                    buildIdentityClipboardText(
                        addedIdentityHashes = visibleAdded,
                        removedIdentityHashes = visibleRemoved,
                        includeAdded = true,
                        includeRemoved = true,
                    ).copyTextToClipboard()
                    project.toast(
                        PluginBundle.get(
                            "vm.memory.diff.copy.identities.toast",
                            visibleAdded.size + visibleRemoved.size,
                        ),
                    )
                },
                enabled = visibleAdded.isNotEmpty() || visibleRemoved.isNotEmpty(),
            ) {
                Text(PluginBundle.get("vm.memory.diff.copy.both"))
            }
            OutlinedButton(
                onClick = {
                    exportIdentityDiffCsv(
                        project = project,
                        row = row,
                        beforeSnapshotId = beforeSnapshotId,
                        afterSnapshotId = afterSnapshotId,
                        addedIdentityHashes = visibleAdded,
                        removedIdentityHashes = visibleRemoved,
                    )
                },
                enabled = visibleAdded.isNotEmpty() || visibleRemoved.isNotEmpty(),
            ) {
                Text(PluginBundle.get("vm.memory.diff.export.csv"))
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IdentityHashList(
                title = PluginBundle.get("vm.memory.diff.hash.title.added", visibleAdded.size, row.identityAdded),
                values = visibleAdded,
                color = JewelTheme.globalColors.text.error,
                onClickHash = { hash ->
                    formatIdentityHash(hash).copyTextToClipboard()
                    project.toast(PluginBundle.get("vm.memory.diff.hash.copy.added.toast", identityHashHex(hash)))
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            IdentityHashList(
                title = PluginBundle.get("vm.memory.diff.hash.title.removed", visibleRemoved.size, row.identityRemoved),
                values = visibleRemoved,
                color = JewelTheme.globalColors.text.info,
                onClickHash = { hash ->
                    formatIdentityHash(hash).copyTextToClipboard()
                    project.toast(PluginBundle.get("vm.memory.diff.hash.copy.removed.toast", identityHashHex(hash)))
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun IdentityHashList(
    title: String,
    values: List<Int>,
    color: androidx.compose.ui.graphics.Color,
    onClickHash: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewLimit = 300
    val preview = remember(values) { values.take(previewLimit) }
    Column(
        modifier = modifier
            .border(1.dp, JewelTheme.globalColors.borders.normal)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = color)
        Text(PluginBundle.get("vm.memory.diff.hash.list.copy.hint"), color = JewelTheme.globalColors.text.info)
        if (preview.isEmpty()) {
            Text(PluginBundle.get("vm.memory.common.empty"), color = JewelTheme.globalColors.text.info)
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            preview.forEach { hash ->
                Text(
                    formatIdentityHash(hash),
                    modifier = Modifier.fillMaxWidth().clickable { onClickHash(hash) },
                )
            }
            if (values.size > preview.size) {
                Text(
                    PluginBundle.get("vm.memory.diff.hash.list.more", values.size - preview.size),
                    color = JewelTheme.globalColors.text.info,
                )
            }
        }
    }
}

@Composable
private fun MemoryTraceTab(
    loadingClasses: Boolean,
    loadingDetails: Boolean,
    error: String?,
    classes: List<TraceClassItem>,
    selectedClass: TraceClassItem?,
    tracedClassIds: Set<String>,
    sampleCount: Int?,
    instanceCount: Int?,
    firstInstanceId: String?,
    retainingPathLines: List<String>,
    onRefreshClasses: () -> Unit,
    onSelectClass: (String) -> Unit,
    onInspect: () -> Unit,
    onEnableTrace: (String) -> Unit,
    onDisableTrace: (String) -> Unit,
) {
    var splitState by remember { mutableStateOf(SplitLayoutState(0.42f)) }
    val searchState = rememberTextFieldState("")
    val query = searchState.text.toString().trim()
    val filteredClasses = remember(classes, query) {
        if (query.isBlank()) classes else {
            val q = query.lowercase()
            classes.filter {
                it.className.lowercase().contains(q) || it.libraryUri.lowercase().contains(q)
            }
        }
    }

    HorizontalSplitLayout(
        modifier = Modifier.fillMaxSize(),
        state = splitState,
        firstPaneMinWidth = 180.dp,
        secondPaneMinWidth = 220.dp,
        first = {
            val listState = rememberLazyListState()
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconActionButton(
                        key = AllIconsKeys.Actions.Refresh,
                        contentDescription = PluginBundle.get("vm.memory.trace.refresh.classes.desc"),
                        onClick = onRefreshClasses,
                    )
                    TextField(
                        state = searchState,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(PluginBundle.get("vm.memory.trace.filter.placeholder")) },
                    )
                }
                Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
                if (loadingClasses) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                            items(filteredClasses.size) { index ->
                                val item = filteredClasses[index]
                                val selected = selectedClass?.classId == item.classId
                                val traced = tracedClassIds.contains(item.classId)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectClass(item.classId) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.className,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (selected) JewelTheme.globalColors.text.normal
                                        else JewelTheme.globalColors.text.info,
                                    )
                                    if (traced) {
                                        Text(PluginBundle.get("vm.memory.trace.badge"), color = JewelTheme.globalColors.text.info)
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
        },
        second = {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (selectedClass == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(PluginBundle.get("vm.memory.trace.select.class"), color = JewelTheme.globalColors.text.info)
                    }
                    return@Column
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        selectedClass.className,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (tracedClassIds.contains(selectedClass.classId)) {
                        OutlinedButton(onClick = { onDisableTrace(selectedClass.classId) }) {
                            Text(PluginBundle.get("vm.memory.trace.disable"))
                        }
                    } else {
                        OutlinedButton(onClick = { onEnableTrace(selectedClass.classId) }) {
                            Text(PluginBundle.get("vm.memory.trace.enable"))
                        }
                    }
                    OutlinedButton(onClick = onInspect) {
                        Text(PluginBundle.get("vm.memory.trace.inspect"))
                    }
                }
                Text(
                    selectedClass.libraryUri.ifBlank { "-" },
                    color = JewelTheme.globalColors.text.info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))
                Divider(Orientation.Horizontal, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                if (loadingDetails) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(16.dp).height(16.dp))
                        Text(PluginBundle.get("vm.memory.trace.loading.details"))
                    }
                } else {
                    Text(PluginBundle.get("vm.memory.trace.sample.count", sampleCount ?: PluginBundle.get("vm.memory.common.empty")))
                    Text(PluginBundle.get("vm.memory.trace.instance.count", instanceCount ?: PluginBundle.get("vm.memory.common.empty")))
                    Text(
                        text = PluginBundle.get("vm.memory.trace.first.instance", firstInstanceId ?: PluginBundle.get("vm.memory.common.empty")),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(PluginBundle.get("vm.memory.common.error", error), color = JewelTheme.globalColors.text.error)
                }

                Spacer(Modifier.height(8.dp))
                Text(PluginBundle.get("vm.memory.trace.retaining.path"), fontWeight = FontWeight.Bold)
                Divider(Orientation.Horizontal, Modifier.fillMaxWidth())

                SelectionContainer {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (retainingPathLines.isEmpty()) {
                            Text(PluginBundle.get("vm.memory.common.empty"), color = JewelTheme.globalColors.text.info)
                        } else {
                            retainingPathLines.forEach {
                                Text(it)
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), pre)
}

private fun snapshotDiffComparator(
    mode: DiffSortMode,
    descending: Boolean,
): Comparator<SnapshotClassDiff> {
    val comparator = when (mode) {
        DiffSortMode.AbsDeltaBytes -> compareBy<SnapshotClassDiff> { abs(it.deltaBytes) }
            .thenBy { abs(it.deltaInstances) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.DeltaBytes -> compareBy<SnapshotClassDiff> { it.deltaBytes }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.AbsDeltaInstances -> compareBy<SnapshotClassDiff> { abs(it.deltaInstances) }
            .thenBy { abs(it.deltaBytes) }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.DeltaInstances -> compareBy<SnapshotClassDiff> { it.deltaInstances }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.AfterBytes -> compareBy<SnapshotClassDiff> { it.afterBytes }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.AddedObjects -> compareBy<SnapshotClassDiff> { it.identityAdded }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.RemovedObjects -> compareBy<SnapshotClassDiff> { it.identityRemoved }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.className }

        DiffSortMode.ClassName -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.className }
    }
    return if (descending) comparator.reversed() else comparator
}

private fun sortModeLabel(mode: DiffSortMode): String {
    return when (mode) {
        DiffSortMode.AbsDeltaBytes -> PluginBundle.get("vm.memory.sort.abs.delta.bytes")
        DiffSortMode.DeltaBytes -> PluginBundle.get("vm.memory.sort.delta.bytes")
        DiffSortMode.AbsDeltaInstances -> PluginBundle.get("vm.memory.sort.abs.delta.instances")
        DiffSortMode.DeltaInstances -> PluginBundle.get("vm.memory.sort.delta.instances")
        DiffSortMode.AfterBytes -> PluginBundle.get("vm.memory.sort.after.bytes")
        DiffSortMode.AddedObjects -> PluginBundle.get("vm.memory.sort.added.obj")
        DiffSortMode.RemovedObjects -> PluginBundle.get("vm.memory.sort.removed.obj")
        DiffSortMode.ClassName -> PluginBundle.get("vm.memory.sort.class")
    }
}

private fun formatSignedLong(value: Long): String {
    if (value > 0L) return "+$value"
    return value.toString()
}

private fun formatSignedBytes(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    return if (bytes > 0) "+${formatBytes(bytes)}" else "-${formatBytes(abs(bytes))}"
}

private fun formatIdentityHash(hash: Int): String {
    return "${identityHashHex(hash)} ($hash)"
}

private fun identityHashHex(hash: Int): String {
    val hex = hash.toUInt().toString(16).uppercase().padStart(8, '0')
    return "0x$hex"
}

private fun buildIdentityClipboardText(
    addedIdentityHashes: List<Int>,
    removedIdentityHashes: List<Int>,
    includeAdded: Boolean,
    includeRemoved: Boolean,
): String {
    val lines = mutableListOf<String>()
    lines += "kind,identity_hash_hex,identity_hash_dec"
    if (includeAdded) {
        addedIdentityHashes.forEach { hash ->
            lines += "added,${identityHashHex(hash)},$hash"
        }
    }
    if (includeRemoved) {
        removedIdentityHashes.forEach { hash ->
            lines += "removed,${identityHashHex(hash)},$hash"
        }
    }
    return lines.joinToString("\n")
}

private fun exportIdentityDiffCsv(
    project: Project,
    row: SnapshotClassDiff,
    beforeSnapshotId: Int,
    afterSnapshotId: Int,
    addedIdentityHashes: List<Int>,
    removedIdentityHashes: List<Int>,
) {
    val descriptor = FileSaverDescriptor(
        PluginBundle.get("vm.memory.diff.export.title"),
        PluginBundle.get("vm.memory.diff.export.desc"),
        "csv",
    )
    val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val homePath = System.getProperty("user.home")
    val baseDir = LocalFileSystem.getInstance().findFileByPath(homePath)
    val defaultFileName = buildDefaultIdentityDiffCsvName(row.className, beforeSnapshotId, afterSnapshotId)
    val wrapper = saveDialog.save(baseDir, defaultFileName)
    if (wrapper == null) {
        project.toast(PluginBundle.get("vm.memory.diff.export.cancelled"))
        return
    }
    val csvContent = buildIdentityDiffCsv(
        row = row,
        beforeSnapshotId = beforeSnapshotId,
        afterSnapshotId = afterSnapshotId,
        addedIdentityHashes = addedIdentityHashes,
        removedIdentityHashes = removedIdentityHashes,
    )
    runCatching {
        wrapper.file.writeText(csvContent)
        VfsUtil.markDirtyAndRefresh(true, true, true, wrapper.virtualFile)
        project.toast(
            PluginBundle.get(
                "vm.memory.diff.export.success",
                wrapper.file.absolutePath,
                addedIdentityHashes.size,
                removedIdentityHashes.size,
            ),
        )
    }.onFailure {
        project.toastWithError(
            PluginBundle.get(
                "vm.memory.diff.export.failed",
                it.message ?: PluginBundle.get("vm.memory.diff.export.unknown.error"),
            ),
        )
    }
}

private fun buildDefaultIdentityDiffCsvName(
    className: String,
    beforeSnapshotId: Int,
    afterSnapshotId: Int,
): String {
    val sanitizedClass = sanitizeFileName(className).take(48).ifBlank { "class" }
    return "memory_identity_diff_${beforeSnapshotId}_${afterSnapshotId}_$sanitizedClass.csv"
}

private fun buildIdentityDiffCsv(
    row: SnapshotClassDiff,
    beforeSnapshotId: Int,
    afterSnapshotId: Int,
    addedIdentityHashes: List<Int>,
    removedIdentityHashes: List<Int>,
): String {
    val lines = mutableListOf<String>()
    lines += "kind,identity_hash_hex,identity_hash_dec,class_name,library_uri,before_snapshot_id,after_snapshot_id"
    addedIdentityHashes.forEach { hash ->
        lines += listOf(
            "added",
            identityHashHex(hash),
            hash.toString(),
            csvEscape(row.className),
            csvEscape(row.libraryUri),
            beforeSnapshotId.toString(),
            afterSnapshotId.toString(),
        ).joinToString(",")
    }
    removedIdentityHashes.forEach { hash ->
        lines += listOf(
            "removed",
            identityHashHex(hash),
            hash.toString(),
            csvEscape(row.className),
            csvEscape(row.libraryUri),
            beforeSnapshotId.toString(),
            afterSnapshotId.toString(),
        ).joinToString(",")
    }
    return lines.joinToString("\n")
}

private fun filterIdentityHashes(values: List<Int>, query: String): List<Int> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return values
    val qHex = q.removePrefix("0x")
    return values.filter { hash ->
        val hexRaw = hash.toUInt().toString(16).lowercase().padStart(8, '0')
        val hexWithPrefix = "0x$hexRaw"
        val dec = hash.toString()
        val unsignedDec = hash.toUInt().toString()
        dec.contains(q) || unsignedDec.contains(q) || hexRaw.contains(qHex) || hexWithPrefix.contains(q)
    }
}

private fun csvEscape(value: String): String {
    if (value.isEmpty()) return ""
    val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!needsQuote) return value
    return "\"${value.replace("\"", "\"\"")}\""
}

private fun sanitizeFileName(value: String): String {
    return value.map { ch ->
        if (ch.isLetterOrDigit() || ch == '-' || ch == '_') ch else '_'
    }.joinToString("").trim('_')
}

@Composable
private fun deltaColor(delta: Long): androidx.compose.ui.graphics.Color {
    return when {
        delta > 0L -> JewelTheme.globalColors.text.error
        delta < 0L -> JewelTheme.globalColors.text.info
        else -> JewelTheme.globalColors.text.normal
    }
}

private fun formatTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatChartSampleTime(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}
