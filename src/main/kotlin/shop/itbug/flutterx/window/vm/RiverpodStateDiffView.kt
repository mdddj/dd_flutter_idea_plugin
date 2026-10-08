package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestFactory
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightVirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import shop.itbug.flutterx.i18n.PluginBundle
import vm.VmService
import vm.devtool.RiverpodHelper
import vm.devtool.RiverpodProviderInfo
import vm.devtool.RiverpodProviderStatus
import vm.devtool.RiverpodState
import vm.devtool.RiverpodStateRenderer
import vm.devtool.isControlFlowException

/**
 * Provider 的状态 diff 视图。
 *
 * 将当前帧的 state 与上一次更新的 state 渲染为文本，并使用 IDE 自带的 diff
 * 编辑器（[DiffRequestPanel]）展示，逐行高亮"哪个属性变了"。
 */
@Composable
fun RiverpodStateDiffView(
    project: Project,
    vmService: VmService,
    state: RiverpodState,
    provider: RiverpodProviderInfo,
    modifier: Modifier = Modifier
) {
    val eval = remember(vmService) { RiverpodHelper.getRiverpodEval(vmService) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var previousText by remember { mutableStateOf<String?>(null) }
    var currentText by remember { mutableStateOf<String?>(null) }
    var previousFrame by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(provider.elementId, state.selectedFrameIndex) {
        loading = true
        error = null
        previousText = null
        currentText = null
        previousFrame = null
        try {
            val evalRef = eval
            if (evalRef == null) {
                error = "Riverpod package not loaded"
                return@LaunchedEffect
            }
            val rendered = withContext(Dispatchers.IO) {
                val source = state.getStateDiffSource(provider.elementId, state.selectedFrameIndex)
                val prevInstance = source?.previousPath
                    ?.let { RiverpodHelper.getStateInstance(evalRef, vmService, it) }
                val currInstance = state.getCurrentStatePath(provider.elementId)
                    ?.let { RiverpodHelper.getStateInstance(evalRef, vmService, it) }

                val prevText = prevInstance
                    ?.let { RiverpodStateRenderer.render(evalRef, vmService, it) }
                val currText = currInstance
                    ?.let { RiverpodStateRenderer.render(evalRef, vmService, it) }
                Triple(source?.previousFrameIndex, prevText, currText)
            }
            previousFrame = rendered.first
            previousText = rendered.second
            currentText = rendered.third
        } catch (e: Exception) {
            if (!isControlFlowException(e)) error = e.message
        } finally {
            loading = false
        }
    }

    val frameStatus = state.getStatusAt(provider.elementId, state.selectedFrameIndex)
    val currentFrameNumber = state.selectedFrameIndex + 1

    when {
        loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(PluginBundle.get("riverpod.state.diff.rendering"))
                }
            }
        }

        error != null -> {
            Box(modifier = modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopStart) {
                Text(error ?: "", color = JewelTheme.globalColors.text.error)
            }
        }

        currentText == null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    PluginBundle.get("riverpod.state.diff.no.state"),
                    color = JewelTheme.globalColors.text.info
                )
            }
        }

        frameStatus == RiverpodProviderStatus.Disposed -> {
            ValueSummary(
                message = PluginBundle.get("riverpod.state.diff.disposed"),
                valueTitle = PluginBundle.get("riverpod.diff.current", currentFrameNumber),
                value = currentText ?: "",
                modifier = modifier
            )
        }

        frameStatus == null || previousText == null || previousText == currentText -> {
            val message = if (previousText == null) {
                PluginBundle.get("riverpod.state.diff.no.previous")
            } else {
                PluginBundle.get("riverpod.state.diff.unchanged")
            }
            ValueSummary(
                message = message,
                valueTitle = PluginBundle.get("riverpod.diff.current", currentFrameNumber),
                value = currentText ?: "",
                modifier = modifier
            )
        }

        isCompactState(previousText ?: "") && isCompactState(currentText ?: "") -> {
            CompactStateDiff(
                previousTitle = PluginBundle.get("riverpod.diff.previous", (previousFrame ?: 0) + 1),
                currentTitle = PluginBundle.get("riverpod.diff.current", currentFrameNumber),
                previous = previousText ?: "",
                current = currentText ?: "",
                modifier = modifier
            )
        }

        else -> {
            StateDiffPanel(
                project = project,
                previousTitle = PluginBundle.get("riverpod.diff.previous", (previousFrame ?: 0) + 1),
                currentTitle = PluginBundle.get("riverpod.diff.current", currentFrameNumber),
                previousText = previousText ?: "",
                currentText = currentText ?: "",
                modifier = modifier
            )
        }
    }
}

private fun isCompactState(text: String): Boolean {
    val lines = text.lineSequence().count { it.isNotBlank() }
    return lines <= 3 && text.length <= 180
}

@Composable
private fun ValueSummary(
    message: String,
    valueTitle: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(message, color = JewelTheme.globalColors.text.info)
        ValueCard(title = valueTitle, value = value, highlight = false, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompactStateDiff(
    previousTitle: String,
    currentTitle: String,
    previous: String,
    current: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        ValueCard(previousTitle, previous, highlight = false, modifier = Modifier.weight(1f))
        Text("→", modifier = Modifier.padding(top = 28.dp), fontWeight = FontWeight.Bold)
        ValueCard(currentTitle, current, highlight = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ValueCard(
    title: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (highlight) Color(0xFF4EC9B0).copy(alpha = 0.16f)
                else JewelTheme.globalColors.panelBackground
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = JewelTheme.globalColors.text.info)
        Text(value.ifBlank { "null" }, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StateDiffPanel(
    project: Project,
    previousTitle: String,
    currentTitle: String,
    previousText: String,
    currentText: String,
    modifier: Modifier = Modifier
) {
    val (panel, parentDisposable) = remember(project) {
        val parent = Disposer.newDisposable("RiverpodDiffPanel")
        val p = DiffManager.getInstance().createRequestPanel(project, parent, null)
        p to parent
    }

    DisposableEffect(Unit) {
        onDispose {
            Disposer.dispose(parentDisposable)
        }
    }

    LaunchedEffect(previousText, currentText, previousTitle, currentTitle) {
        val prevFile = LightVirtualFile(
            previousTitle,
            PlainTextFileType.INSTANCE,
            previousText
        )
        val currFile = LightVirtualFile(
            currentTitle,
            PlainTextFileType.INSTANCE,
            currentText
        )
        val request = DiffRequestFactory.getInstance().createFromFiles(project, prevFile, currFile)
        panel.setRequest(request)
    }

    SwingPanel(
        factory = { panel.component },
        modifier = modifier.fillMaxSize()
    )
}
