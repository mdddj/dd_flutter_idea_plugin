package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.skiko.Cursor
import shop.itbug.flutterx.common.dart.FlutterAppInstance
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.util.MyFileUtil
import vm.VmService
import vm.element.IsolateRef
import vm.element.VM
import vm.getVm
import vm.setInspectorOverlay
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.SwingUtilities
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun DartVmStatusComponent(project: Project) {
    FlutterAppsTabComponent(project) {
        FlutterAppStatusPanel(project, it)
    }

}

@Composable
private fun FlutterAppStatusPanel(project: Project, app: FlutterAppInstance) {
    val vmService = app.vmService
    var vm by remember { mutableStateOf<VM?>(null) }
    LaunchedEffect(app) {
        vmService.runInScope {
            vm = getVm()
        }
    }

    if (vm != null) {
        HorizontalSplitLayout(
            modifier = Modifier.fillMaxSize().border(1.dp, color = JewelTheme.globalColors.borders.normal),
            first = {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VmInfoDisplay(vm!!)
                    OutlinedButton({
                        MyFileUtil.showJsonInEditor(project, vm!!.json.toString())
                    }, enabled = vm != null) {
                        Text(PluginBundle.get("open.in.editor"))
                    }
                }
            },
            second = {
                VmMemoryDisplay(app,vm!!, vmService, project)
            },
            state = rememberSplitLayoutState(0.65f)
        )
    } else {
        CircularProgressIndicator()
    }

}

@Composable
private fun VmMemoryDisplay(app: FlutterAppInstance, vm: VM, vmService: VmService, project: Project) {
    var vmInfo by remember(app) { mutableStateOf(vm) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        vmInfo = vmService.getVm()
    }
    LaunchedEffect(app){
        refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(PluginBundle.get("vm.status.quick.actions"), color = JewelTheme.globalColors.text.normal)
            InspectorStateComponent(vmService, project)
        }
        Divider(
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(PluginBundle.get("vm.status.memory.section"))
            IconActionButton(
                AllIconsKeys.Actions.Refresh,
                onClick = { scope.launch { refresh() } },
                contentDescription = PluginBundle.get("vm.status.refresh.memory.desc")
            )
        }
        KeyValueRow(PluginBundle.get("vm.status.current.memory"), formatBytes(vmInfo.getCurrentMemory()))
        KeyValueRow(PluginBundle.get("vm.status.current.rss"), formatBytes(vmInfo.getCurrentRSS()))
        KeyValueRow(PluginBundle.get("vm.status.max.rss"), formatBytes(vmInfo.getMaxRSS()))
    }
}

@Composable
private fun VmInfoDisplay(vmInfo: VM) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle(PluginBundle.get("vm.status.vm.info"))
        KeyValueRow(PluginBundle.get("vm.status.type"), vmInfo.type)
        KeyValueRow(PluginBundle.get("vm.status.name"), vmInfo.getName())
        KeyValueRow(PluginBundle.get("vm.status.version"), vmInfo.getVersion())
        KeyValueRow(PluginBundle.get("vm.status.pid"), vmInfo.getPid().toString())
        KeyValueRow(PluginBundle.get("vm.status.start.time"), formatTimestamp(vmInfo.getStartTime()))
        KeyValueRow(PluginBundle.get("vm.status.embedder"), vmInfo.getEmbedder())

        OpenGroupPanel(PluginBundle.get("vm.status.host.target")) {
            KeyValueRow(PluginBundle.get("vm.status.os"), vmInfo.getOperatingSystem())
            KeyValueRow(PluginBundle.get("vm.status.host.cpu"), vmInfo.getHostCPU())
            KeyValueRow(PluginBundle.get("vm.status.target.cpu"), vmInfo.getTargetCPU())
            KeyValueRow(PluginBundle.get("vm.status.architecture"), PluginBundle.get("vm.status.arch.bits", vmInfo.getArchitectureBits()))
        }

        OpenGroupPanel(PluginBundle.get("vm.status.enabled.features")) {
            SelectionContainer {
                Text(vmInfo.getFeatures())
            }
        }
        OpenGroupPanel(PluginBundle.get("vm.status.isolates.group", vmInfo.getIsolates().size())) {
            vmInfo.getIsolates().forEach { isolate ->
                IsolateInfoCard(isolate)
            }
        }
        OpenGroupPanel(PluginBundle.get("vm.status.system.isolates.group", vmInfo.getSystemIsolates().size())) {
            vmInfo.getSystemIsolates().forEach { isolate ->
                IsolateInfoCard(isolate)
            }
        }

    }

}


@Composable
private fun KeyValueRow(
    label: String,
    value: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$label:",
            modifier = Modifier.width(140.dp),
        )
        if (content != null) {
            content()
        } else if (value != null) {
            Text(value)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = JewelTheme.globalColors.text.normal,
        fontSize = 15.sp
    )
}

@Composable
private fun IsolateInfoCard(isolate: IsolateRef) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        SelectionContainer {
            Column(modifier = Modifier.padding(12.dp)) {
                KeyValueRow(PluginBundle.get("vm.status.name"), isolate.getName())
                KeyValueRow(PluginBundle.get("vm.status.isolate.id"), isolate.getId())
                KeyValueRow(PluginBundle.get("vm.status.isolate.number"), isolate.getNumber())
                KeyValueRow(PluginBundle.get("vm.status.isolate.system"), isolate.getIsSystemIsolate().toString())
            }
        }
    }
}


private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / 1024.0.pow(exp.toDouble()), pre)
}

@Composable
private fun OpenGroupPanel(title: String, child: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    GroupHeader(
        text = title,
        modifier =
            Modifier.clickable(indication = null, interactionSource = interactionSource) { open = !open }
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
        startComponent = {
            if (open) {
                Icon(AllIconsKeys.General.ChevronDown, PluginBundle.get("vm.status.chevron.desc"))
            } else {
                Icon(AllIconsKeys.General.ChevronRight, PluginBundle.get("vm.status.chevron.desc"))
            }
        },
    )
    if (open) {
        Column(modifier = Modifier.padding(24.dp, 4.dp)) {
            child.invoke()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InspectorStateComponent(vmService: VmService, project: Project) {
    val manager = vmService.inspectorManager
    val isSelect by manager.overlayState.collectAsState()
    LaunchedEffect(vmService, manager) {
        manager.scope.launch {
            manager.navigationEvents.collect { result ->
                SwingUtilities.invokeLater {
                    MyFileUtil.openFile(project, result.fileUri, result.line, result.column)
                }
            }
        }
    }
    ToggleableIconActionButton(
        key = AllIconsKeys.General.Locate,
        contentDescription = PluginBundle.get("vm.status.inspector.toggle.desc"),
        value = isSelect,
        onValueChange = {
            manager.scope.launch {
                vmService.setInspectorOverlay(vmService.getMainIsolateId(), !isSelect)
            }
        }
    ) {
        val stateText = if (isSelect) PluginBundle.get("vm.status.inspector.on") else PluginBundle.get("vm.status.inspector.off")
        Text(PluginBundle.get("vm.status.inspector.state", stateText))
    }
}
