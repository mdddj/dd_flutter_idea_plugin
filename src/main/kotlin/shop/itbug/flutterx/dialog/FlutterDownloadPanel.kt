package shop.itbug.flutterx.dialog

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.i18n.PluginBundle
import java.awt.Dimension
import java.io.File
import javax.swing.Action
import javax.swing.JComponent

private const val MAC_URL = "https://storage.googleapis.com/flutter_infra_release/releases/releases_macos.json"
private const val WIN_URL = "https://storage.googleapis.com/flutter_infra_release/releases/releases_windows.json"
private const val LINUX_URL = "https://storage.googleapis.com/flutter_infra_release/releases/releases_linux.json"

// 数据模型
data class FlutterReleasesResponse(
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("current_release") val currentRelease: CurrentRelease,
    val releases: List<FlutterRelease>
)

data class CurrentRelease(
    val beta: String?,
    val dev: String?,
    val stable: String?
)

data class FlutterRelease(
    val hash: String,
    val channel: String,
    val version: String,
    @SerializedName("dart_sdk_version") val dartSdkVersion: String?,
    @SerializedName("dart_sdk_arch") val dartSdkArch: String?,
    @SerializedName("release_date") val releaseDate: String?,
    val archive: String,
    val sha256: String?
) {
    val displayName: String
        get() = "v$version (${dartSdkArch ?: "unknown"})"
}

// 平台类型
enum class PlatformType(val displayName: String, val url: String) {
    MACOS("macOS", MAC_URL),
    WINDOWS("Windows", WIN_URL),
    LINUX("Linux", LINUX_URL)
}

// 下载状态
sealed interface FlutterDownloadState {
    data object Idle : FlutterDownloadState
    data object Downloading : FlutterDownloadState
    data class Progress(val fraction: Double, val text: String) : FlutterDownloadState
    data class Success(val file: File) : FlutterDownloadState
    data class Error(val message: String) : FlutterDownloadState
}

// 加载状态
sealed interface LoadingState<out T> {
    data object Idle : LoadingState<Nothing>
    data object Loading : LoadingState<Nothing>
    data class Success<T>(val data: T) : LoadingState<T>
    data class Error(val message: String) : LoadingState<Nothing>
}

/**
 * Flutter 版本下载器面板
 */
@Composable
fun FlutterDownloadPanel(project: Project, onClose: () -> Unit = {}) {
    var selectedPlatformIndex by remember { mutableIntStateOf(0) }
    var releasesState by remember { mutableStateOf<LoadingState<FlutterReleasesResponse>>(LoadingState.Idle) }
    var selectedReleaseIndex by remember { mutableIntStateOf(-1) }
    var downloadPath by remember { mutableStateOf("") }
    var downloadState by remember { mutableStateOf<FlutterDownloadState>(FlutterDownloadState.Idle) }
    var selectedChannelIndex by remember { mutableIntStateOf(0) }

    val selectedPlatform = PlatformType.entries[selectedPlatformIndex]
    val channels = listOf("stable", "beta", "dev")
    val selectedChannel = channels[selectedChannelIndex]

    // 过滤后的版本列表
    val filteredReleases = remember(releasesState, selectedChannel) {
        when (val state = releasesState) {
            is LoadingState.Success -> state.data.releases
                .filter { it.channel == selectedChannel }
                .take(30)
            else -> emptyList()
        }
    }

    val selectedRelease = filteredReleases.getOrNull(selectedReleaseIndex)

    // 当平台改变时加载版本列表
    LaunchedEffect(selectedPlatform) {
        releasesState = LoadingState.Loading
        selectedReleaseIndex = -1
        try {
            val response = withContext(Dispatchers.IO) {
                val json = HttpRequests.request(selectedPlatform.url).readString()
                Gson().fromJson(json, FlutterReleasesResponse::class.java)
            }
            releasesState = LoadingState.Success(response)
        } catch (e: Exception) {
            releasesState = LoadingState.Error(e.message ?: "Failed to load releases")
        }
    }

    // Channel 改变时重置版本选择
    LaunchedEffect(selectedChannel) {
        selectedReleaseIndex = -1
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text("Flutter SDK Downloader", fontWeight = FontWeight.Bold)
        Divider(Orientation.Horizontal)

        // 平台选择 - SegmentedControl
        PlatformSelector(
            selectedIndex = selectedPlatformIndex,
            onPlatformSelected = {
                selectedPlatformIndex = it
                downloadState = FlutterDownloadState.Idle
            }
        )

        // Channel 选择 - SegmentedControl
        ChannelSelector(
            selectedIndex = selectedChannelIndex,
            onChannelSelected = { selectedChannelIndex = it }
        )

        // 版本选择 - ListComboBox
        VersionSelector(
            releasesState = releasesState,
            filteredReleases = filteredReleases,
            selectedIndex = selectedReleaseIndex,
            onReleaseSelected = { selectedReleaseIndex = it }
        )

        // 下载路径选择
        AnimatedVisibility(
            visible = selectedRelease != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            DownloadPathSelector(
                project = project,
                downloadPath = downloadPath,
                onPathSelected = { downloadPath = it }
            )
        }

        Spacer(Modifier.weight(1f))

        // 下载状态和按钮
        DownloadSection(
            project = project,
            selectedRelease = selectedRelease,
            releasesState = releasesState,
            downloadPath = downloadPath,
            downloadState = downloadState,
            onDownloadStateChange = { downloadState = it },
            onClose = onClose
        )
    }
}

@Composable
private fun PlatformSelector(
    selectedIndex: Int,
    onPlatformSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Platform:", modifier = Modifier.alpha(0.8f))

        val buttons = remember(selectedIndex) {
            PlatformType.entries.mapIndexed { index, platform ->
                SegmentedControlButtonData(
                    selected = index == selectedIndex,
                    content = { _ -> Text(platform.displayName) },
                    onSelect = { onPlatformSelected(index) }
                )
            }
        }

        SegmentedControl(buttons = buttons, enabled = true)
    }
}

@Composable
private fun ChannelSelector(
    selectedIndex: Int,
    onChannelSelected: (Int) -> Unit
) {
    val channels = listOf("Stable", "Beta", "Dev")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Channel:", modifier = Modifier.alpha(0.8f))

        val buttons = remember(selectedIndex) {
            channels.mapIndexed { index, channel ->
                SegmentedControlButtonData(
                    selected = index == selectedIndex,
                    content = { _ -> Text(channel) },
                    onSelect = { onChannelSelected(index) }
                )
            }
        }

        SegmentedControl(buttons = buttons, enabled = true)
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
private fun VersionSelector(
    releasesState: LoadingState<FlutterReleasesResponse>,
    filteredReleases: List<FlutterRelease>,
    selectedIndex: Int,
    onReleaseSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Version:", modifier = Modifier.alpha(0.8f))

        when (releasesState) {
            is LoadingState.Idle -> {
                Text("Please select a platform first", modifier = Modifier.alpha(0.5f))
            }
            is LoadingState.Loading -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text("Loading versions...")
                }
            }
            is LoadingState.Error -> {
                Text(releasesState.message, color = JewelTheme.globalColors.text.error)
            }
            is LoadingState.Success -> {
                if (filteredReleases.isEmpty()) {
                    Text("No releases found", modifier = Modifier.alpha(0.5f))
                } else {
                    ListComboBox(
                        items = filteredReleases,
                        selectedIndex = selectedIndex,
                        onSelectedItemChange = { index -> onReleaseSelected(index) },
                        modifier = Modifier.fillMaxWidth(),
                        maxPopupHeight = 250.dp,
                        itemKeys = { index, _ -> index },
                        itemContent = { release, isSelected, isActive ->
                            SimpleListItem(
                                text = "${release.displayName} - ${release.releaseDate?.substringBefore("T") ?: ""}",
                                isSelected = isSelected,
                                isActive = isActive,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadPathSelector(
    project: Project,
    downloadPath: String,
    onPathSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Download Location:", modifier = Modifier.alpha(0.8f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val displayPath = if (downloadPath.isEmpty()) "Select download folder..." else downloadPath
            Text(
                displayPath,
                modifier = Modifier.weight(1f).alpha(if (downloadPath.isEmpty()) 0.5f else 1f)
            )
            OutlinedButton(onClick = {
                val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                descriptor.title = "Select Download Location"
                val chooser = FileChooserFactory.getInstance().createPathChooser(descriptor, project, null)
                chooser.choose(null) { files ->
                    files.firstOrNull()?.let { onPathSelected(it.path) }
                }
            }) {
                Text("Browse...")
            }
        }
    }
}

@Composable
private fun DownloadSection(
    project: Project,
    selectedRelease: FlutterRelease?,
    releasesState: LoadingState<FlutterReleasesResponse>,
    downloadPath: String,
    downloadState: FlutterDownloadState,
    onDownloadStateChange: (FlutterDownloadState) -> Unit,
    onClose: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 下载进度
        AnimatedVisibility(
            visible = downloadState is FlutterDownloadState.Progress || downloadState is FlutterDownloadState.Downloading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (val state = downloadState) {
                    is FlutterDownloadState.Progress -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = state.fraction.toFloat(),
                            animationSpec = tween(100)
                        )
                        HorizontalProgressBar(
                            progress = animatedProgress,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Text(
                            "${(state.fraction * 100).toInt()}% - ${state.text}",
                            modifier = Modifier.alpha(0.7f)
                        )
                    }
                    is FlutterDownloadState.Downloading -> {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text("Preparing download...", modifier = Modifier.alpha(0.7f))
                    }
                    else -> {}
                }
            }
        }

        // 成功状态
        AnimatedVisibility(
            visible = downloadState is FlutterDownloadState.Success,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (downloadState is FlutterDownloadState.Success) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        key = AllIconsKeys.General.InspectionsOK,
                        contentDescription = "Success"
                    )
                    Text("Download completed!")
                    Link("Open folder",{
                        BrowserUtil.browse(downloadState.file.parentFile)
                    })
                }
            }
        }

        // 错误状态
        AnimatedVisibility(
            visible = downloadState is FlutterDownloadState.Error,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (downloadState is FlutterDownloadState.Error) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        key = AllIconsKeys.General.ErrorDialog,
                        contentDescription = "Error"
                    )
                    Text(downloadState.message, color = JewelTheme.globalColors.text.error)
                }
            }
        }

        // 选中版本信息
        selectedRelease?.let { release ->
            Text(
                "Selected: Flutter ${release.version} (${release.channel})",
                modifier = Modifier.alpha(0.6f)
            )
        }

        Divider(Orientation.Horizontal)

        // 下载按钮
        val canDownload = selectedRelease != null &&
            downloadPath.isNotEmpty() &&
            downloadState !is FlutterDownloadState.Downloading &&
            downloadState !is FlutterDownloadState.Progress

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.weight(1f))

            if (downloadState is FlutterDownloadState.Error) {
                OutlinedButton(onClick = { onDownloadStateChange(FlutterDownloadState.Idle) }) {
                    Text(PluginBundle.get("retry"))
                }
            }

            OutlinedButton(onClick = onClose) {
                Text(PluginBundle.get("close"))
            }

            DefaultButton(
                onClick = {
                    if (selectedRelease != null && releasesState is LoadingState.Success) {
                        startDownload(
                            project = project,
                            baseUrl = releasesState.data.baseUrl,
                            release = selectedRelease,
                            downloadPath = downloadPath,
                            onStateChange = onDownloadStateChange,
                            onClose = onClose
                        )
                    }
                },
                enabled = canDownload
            ) {
                Text(PluginBundle.get("download"))
            }
        }
    }
}

private fun startDownload(
    project: Project,
    baseUrl: String,
    release: FlutterRelease,
    downloadPath: String,
    onStateChange: (FlutterDownloadState) -> Unit,
    onClose: () -> Unit
) {
    val downloadUrl = "$baseUrl/${release.archive}"
    val fileName = release.archive.substringAfterLast("/")
    val targetFile = File(downloadPath, fileName)

    onStateChange(FlutterDownloadState.Downloading)

    object : Task.Backgroundable(project, "Downloading Flutter ${release.version}", true) {
        override fun run(indicator: ProgressIndicator) {
            indicator.isIndeterminate = false
            indicator.text = "Downloading $fileName"

            try {
                HttpRequests.request(downloadUrl).connect { request ->
                    request.connection.connectTimeout = 30000
                    request.connection.readTimeout = 30000

                    val connection = request.connection
                    val contentLength = connection.contentLengthLong

                    if (contentLength > 0) {
                        connection.inputStream.use { input ->
                            targetFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytesRead = 0L

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    if (indicator.isCanceled) {
                                        throw InterruptedException("Download cancelled")
                                    }
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead

                                    val fraction = totalBytesRead.toDouble() / contentLength
                                    indicator.fraction = fraction

                                    val downloadedMB = totalBytesRead / (1024.0 * 1024.0)
                                    val totalMB = contentLength / (1024.0 * 1024.0)
                                    val text = "%.1f MB / %.1f MB".format(downloadedMB, totalMB)

                                    onStateChange(FlutterDownloadState.Progress(fraction, text))
                                }
                            }
                        }
                    } else {
                        request.saveToFile(targetFile, indicator)
                    }
                }
            } catch (e: Exception) {
                throw e
            }
        }

        override fun onSuccess() {
            onStateChange(FlutterDownloadState.Success(targetFile))
            // 下载成功后关闭对话框
            onClose()
        }

        override fun onThrowable(error: Throwable) {
            targetFile.delete()
            onStateChange(FlutterDownloadState.Error(error.message ?: "Download failed"))
        }

        override fun onCancel() {
            targetFile.delete()
            onStateChange(FlutterDownloadState.Error("Download cancelled"))
        }
    }.queue()
}

/// flutter下载器
class FlutterDownloadDialog(val project: Project) : DialogWrapper(project) {
    init {
        super.init()
        title = "Flutter Downloader"
    }

    override fun createCenterPanel(): JComponent {
        return JewelComposePanel({
            preferredSize = Dimension(450, 500)
        }) {
            FlutterDownloadPanel(project) {
                close(OK_EXIT_CODE)
            }
        }
    }

    override fun createActions(): Array<out Action> = emptyArray()
}

/// 启动 flutter下载器
class FlutterDownloaderAction : AnAction(){
    override fun actionPerformed(p0: AnActionEvent) {
        p0.project?.let { FlutterDownloadDialog(it) }?.show()
    }

}
