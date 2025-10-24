package shop.itbug.fluttercheckversionx.dialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import java.awt.Dimension
import java.io.File
import javax.swing.Action
import javax.swing.JComponent

data class DownloadDescription(val title: String, val size: String)

private sealed interface DownloadState {
    object Idle : DownloadState
    object Downloading : DownloadState
    data class Success(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}


abstract class DownloadSource {
    abstract val url: String
    abstract fun getSaveToFile(): String
    abstract val description: DownloadDescription

    fun getFile() = File(getSaveToFile())
    fun fileIsExists(): Boolean {
        return getFile().exists()
    }

    fun getFileBuffReader() = if (fileIsExists()) getFile().bufferedReader() else null
}

private data class DownloadJob(
    val source: DownloadSource,
    val state: MutableStateFlow<DownloadState> = MutableStateFlow(DownloadState.Idle)
)

class DownloadSourceDialog(val project: Project, sources: List<DownloadSource>) : DialogWrapper(true) {


    private val downloadJobs = sources.map { DownloadJob(it) }


    private fun createDownloadTask(job: DownloadJob): Task.Modal {
        val saveToFile = job.source.getSaveToFile()
        return object : Task.Modal(project, PluginBundle.get("downloading"), true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "${PluginBundle.get("downloading")}  ${job.source.url}"
                HttpRequests.request(job.source.url).connect {
                    it.saveToFile(File(saveToFile), indicator)
                }
            }

            override fun onSuccess() {
                job.state.value = DownloadState.Success(File(saveToFile))
            }

            override fun onThrowable(error: Throwable) {
                job.state.value = DownloadState.Error(error.message ?: "An unknown error occurred")
            }
        }
    }


    init {
        super.init()
        title = if (sources.size == 1) "FlutterX ${PluginBundle.get("download")}" else "FlutterX Multi-Download"
        setSize(550, 350)
    }

    override fun createCenterPanel(): JComponent {
        return JewelComposePanel({
            preferredSize = Dimension(550, 350)
        }) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
            ) {
                MultiDownloadContent(
                    jobs = downloadJobs,
                    onDownload = ::startDownload,
                    onRetry = ::startDownload,
                    onOpenFile = { BrowserUtil.browse(File(it.parent)) },
                    onClose = { close(OK_EXIT_CODE) }
                )
            }
        }
    }

    private fun startDownload(job: DownloadJob) {
        job.state.value = DownloadState.Downloading
        createDownloadTask(job).queue()
    }

    override fun createActions(): Array<out Action> = emptyArray()
}

@Composable
private fun MultiDownloadContent(
    jobs: List<DownloadJob>,
    onDownload: (DownloadJob) -> Unit,
    onRetry: (DownloadJob) -> Unit,
    onOpenFile: (File) -> Unit,
    onClose: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(jobs) { job ->
                DownloadItemRow(
                    job = job,
                    onDownload = { onDownload(job) },
                    onRetry = { onRetry(job) },
                    onOpenFile = onOpenFile
                )
                Divider(orientation = Orientation.Horizontal)
            }
        }

        val allSuccess = jobs.all { j ->
            val state by j.state.collectAsState()
            state is DownloadState.Success
        }

        if (allSuccess) {
            Text("${PluginBundle.get("download_all_completed")}!")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isIdle = jobs.any { j ->
                val state by j.state.collectAsState()
                state is DownloadState.Idle
            }
            if (isIdle) {
                DefaultButton(onClick = { jobs.filter { it.state.value is DownloadState.Idle }.forEach(onDownload) }) {
                    Text(PluginBundle.get("download_all"))
                }
            }
            DefaultButton(onClick = onClose) {
                Text(PluginBundle.get("close"))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadItemRow(
    job: DownloadJob,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    onOpenFile: (File) -> Unit
) {
    val state by job.state.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            job.source.description.title + " (${job.source.description.size})",
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )

        when (val s = state) {
            is DownloadState.Idle -> {
                DefaultButton(onClick = onDownload, modifier = Modifier.width(100.dp)) {
                    Text(PluginBundle.get("download"))
                }
            }

            is DownloadState.Downloading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(PluginBundle.get("downloading"))
                }
            }

            is DownloadState.Success -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        key = AllIconsKeys.General.InspectionsOK,
                        contentDescription = "Success",
                    )
                    Link(PluginBundle.get("download_open_folder"), {
                        onOpenFile(s.file)
                    })
                }
            }

            is DownloadState.Error -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 250.dp)
                ) {
                    Icon(
                        key = AllIconsKeys.General.ErrorDialog,
                        contentDescription = "Error",
                    )
                    Tooltip({
                        Text(s.message)
                    }) {
                        Text(
                            s.message,
                            color = JewelTheme.globalColors.text.info,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(onClick = onRetry) {
                        Text(PluginBundle.get("retry"))
                    }
                }
            }
        }
    }
}