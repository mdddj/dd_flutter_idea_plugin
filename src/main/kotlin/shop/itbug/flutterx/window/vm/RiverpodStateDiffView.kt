package shop.itbug.flutterx.window.vm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

    LaunchedEffect(provider.elementId, state.selectedFrameIndex) {
        loading = true
        error = null
        previousText = null
        currentText = null
        try {
            val evalRef = eval
            if (evalRef == null) {
                error = "Riverpod package not loaded"
                return@LaunchedEffect
            }
            val (prev, curr) = withContext(Dispatchers.IO) {
                val paths = state.getStateDiffPaths(provider.elementId, state.selectedFrameIndex)
                val prevInstance = paths?.first
                    ?.let { RiverpodHelper.getStateInstance(evalRef, vmService, it) }
                val currInstance = state.getCurrentStatePath(provider.elementId)
                    ?.let { RiverpodHelper.getStateInstance(evalRef, vmService, it) }

                val prevText = prevInstance
                    ?.let { RiverpodStateRenderer.render(evalRef, vmService, it) }
                val currText = currInstance
                    ?.let { RiverpodStateRenderer.render(evalRef, vmService, it) }
                prevText to currText
            }
            previousText = prev
            currentText = curr
        } catch (e: Exception) {
            if (!isControlFlowException(e)) error = e.message
        } finally {
            loading = false
        }
    }

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

        else -> {
            // 没有 previous 时左侧为空，diff 编辑器仍然可以展示"新增"状态
            StateDiffPanel(
                project = project,
                providerName = provider.name,
                frameIndex = state.selectedFrameIndex,
                previousText = previousText ?: "",
                currentText = currentText ?: "",
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StateDiffPanel(
    project: Project,
    providerName: String,
    frameIndex: Int,
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

    LaunchedEffect(previousText, currentText) {
        val prevFile = LightVirtualFile(
            "Frame ${frameIndex - 1} · $providerName",
            PlainTextFileType.INSTANCE,
            previousText
        )
        val currFile = LightVirtualFile(
            "Frame $frameIndex · $providerName",
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
