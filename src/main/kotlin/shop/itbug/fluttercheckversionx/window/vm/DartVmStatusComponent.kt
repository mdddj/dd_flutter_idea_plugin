package shop.itbug.fluttercheckversionx.window.vm

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
import shop.itbug.fluttercheckversionx.common.dart.FlutterAppInstance
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.MyFileUtil
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
            modifier = Modifier.fillMaxWidth().border(1.dp, color = JewelTheme.globalColors.borders.normal),
            first = {
                Column(
                    modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
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
                VmMemoryDisplay(vm!!, vmService, project)
            },
            state = rememberSplitLayoutState(0.5f)
        )
    } else {
        CircularProgressIndicator()
    }

}

@Composable
private fun VmMemoryDisplay(vm: VM, vmService: VmService, project: Project) {
    var vmInfo by remember { mutableStateOf(vm) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        vmInfo = vmService.getVm()
    }



    Column(
        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Actions", color = JewelTheme.globalColors.text.normal)
            InspectorStateComponent(vmService, project)
        }
        Divider(
            orientation = Orientation.Horizontal,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle("Memory")
            IconActionButton(
                AllIconsKeys.Actions.Refresh,
                onClick = { scope.launch { refresh() } },
                contentDescription = "Refresh VM Memory"
            )
        }
        KeyValueRow("Current Memory", formatBytes(vmInfo.getCurrentMemory()))
        KeyValueRow("Current RSS", formatBytes(vmInfo.getCurrentRSS()))
        KeyValueRow("Max RSS", formatBytes(vmInfo.getMaxRSS()))
    }
}

@Composable
private fun VmInfoDisplay(vmInfo: VM) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionTitle("VM Information")
        KeyValueRow("Type", vmInfo.type)
        KeyValueRow("Name", vmInfo.getName())
        KeyValueRow("Version", vmInfo.getVersion())
        KeyValueRow("PID", vmInfo.getPid().toString())
        KeyValueRow("Start Time", formatTimestamp(vmInfo.getStartTime()))
        KeyValueRow("Embedder", vmInfo.getEmbedder())

        OpenGroupPanel("Host & Target") {
            KeyValueRow("OS", vmInfo.getOperatingSystem())
            KeyValueRow("Host CPU", vmInfo.getHostCPU())
            KeyValueRow("Target CPU", vmInfo.getTargetCPU())
            KeyValueRow("Architecture", "${vmInfo.getArchitectureBits()}-bit")
        }

        OpenGroupPanel("Enabled Features") {
            SelectionContainer {
                Text(vmInfo.getFeatures())
            }
        }
        OpenGroupPanel("Isolates (${vmInfo.getIsolates().size()})") {
            vmInfo.getIsolates().forEach { isolate ->
                IsolateInfoCard(isolate)
            }
        }
        OpenGroupPanel("System Isolates (${vmInfo.getSystemIsolates().size()})") {
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
                KeyValueRow("Name", isolate.getName())
                KeyValueRow("ID", isolate.getId())
                KeyValueRow("Number", isolate.getNumber())
                KeyValueRow("Is System", isolate.getIsSystemIsolate().toString())
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
                Icon(AllIconsKeys.General.ChevronDown, "Chevron")
            } else {
                Icon(AllIconsKeys.General.ChevronRight, "Chevron")
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
    LaunchedEffect(manager) {
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
        contentDescription = "",
        value = isSelect,
        onValueChange = {
            manager.scope.launch {
                vmService.setInspectorOverlay(vmService.getMainIsolateId(), !isSelect)
            }
        }
    ) {
        Text("Inspector: ${if(isSelect) "On" else "Off"}")
    }
}