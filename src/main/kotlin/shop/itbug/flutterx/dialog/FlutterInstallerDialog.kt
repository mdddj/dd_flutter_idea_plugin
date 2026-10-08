package shop.itbug.flutterx.dialog

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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.supervisorScope
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.ListComboBox
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.flutterx.i18n.PluginBundle
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent

/** Installs an already-built Flutter artifact on an attached Android or iOS device. */
class FlutterInstallerDialog(private val project: Project) : DialogWrapper(project, true) {

    init {
        title = PluginBundle.get("flutter.installer.title")
        setCancelButtonText(PluginBundle.get("close"))
        init()
    }

    override fun createCenterPanel(): JComponent = JewelComposePanel(
        true,
        { preferredSize = Dimension(680, 460) },
    ) {
        FlutterInstallerContent(project)
    }

    override fun createActions(): Array<out Action> = arrayOf(cancelAction)

    override fun getDimensionServiceKey(): String = "FlutterX.FlutterInstallerDialog"
}

@Composable
private fun FlutterInstallerContent(project: Project) {
    val scope = rememberCoroutineScope()

    var artifacts by remember { mutableStateOf<List<FlutterInstallArtifact>>(emptyList()) }
    var devices by remember { mutableStateOf<List<FlutterInstallDevice>>(emptyList()) }
    var artifactIndex by remember { mutableIntStateOf(-1) }
    var deviceIndex by remember { mutableIntStateOf(-1) }
    var scanStatusMessage by remember { mutableStateOf("") }
    var scanStatusIsError by remember { mutableStateOf(false) }
    var installStatusMessage by remember { mutableStateOf("") }
    var installStatusIsError by remember { mutableStateOf(false) }
    var outputText by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }

    val outputState = rememberTextFieldState()

    fun setScanStatus(message: String, isError: Boolean = false) {
        scanStatusMessage = message
        scanStatusIsError = isError
    }

    fun setInstallStatus(message: String, isError: Boolean = false) {
        installStatusMessage = message
        installStatusIsError = isError
    }

    fun refresh() {
        if (scanning) return
        val root = project.guessProjectDir()?.toNioPath()
        if (root == null) {
            setScanStatus(PluginBundle.get("flutter.installer.no.project"), isError = true)
            return
        }

        scanning = true
        setScanStatus(PluginBundle.get("flutter.installer.scanning"))

        scope.launch {
            try {
                var artifactFailure: Throwable? = null
                var deviceFailure: Throwable? = null
                supervisorScope {
                    val artifactScan = async(Dispatchers.IO) {
                        runInterruptible { FlutterInstallerSupport.scanArtifacts(root) }
                    }
                    val deviceScan = async(Dispatchers.IO) {
                        runInterruptible { FlutterInstallerSupport.scanDevices(project) }
                    }

                    launch {
                        try {
                            val foundArtifacts = artifactScan.await()
                            val selectedArtifact = artifacts.getOrNull(artifactIndex)
                            artifacts = foundArtifacts
                            artifactIndex = selectedArtifact
                                ?.let(foundArtifacts::indexOf)
                                ?.takeIf { it >= 0 }
                                ?: if (foundArtifacts.isEmpty()) -1 else 0
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            artifactFailure = e
                        }
                    }

                    launch {
                        try {
                            val foundDevices = deviceScan.await()
                            val selectedDevice = devices.getOrNull(deviceIndex)
                            devices = foundDevices
                            deviceIndex = selectedDevice
                                ?.let(foundDevices::indexOf)
                                ?.takeIf { it >= 0 }
                                ?: if (foundDevices.isEmpty()) -1 else 0
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            deviceFailure = e
                        }
                    }
                }

                val failures = listOfNotNull(artifactFailure, deviceFailure)
                if (failures.isEmpty()) {
                    setScanStatus(
                        PluginBundle.get("flutter.installer.scan.summary", artifacts.size, devices.size),
                        isError = artifacts.isEmpty() || devices.isEmpty(),
                    )
                } else {
                    val details = failures.mapNotNull { it.message }.filter { it.isNotBlank() }.joinToString("; ")
                    setScanStatus(
                        details.ifBlank { PluginBundle.get("flutter.installer.scan.failed") },
                        isError = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                setScanStatus(e.message ?: PluginBundle.get("flutter.installer.scan.failed"), isError = true)
            } finally {
                scanning = false
            }
        }
    }

    fun install() {
        if (installing) return
        val artifact = artifacts.getOrNull(artifactIndex)
        val device = devices.getOrNull(deviceIndex)
        if (artifact == null || device == null) {
            setInstallStatus(PluginBundle.get("flutter.installer.select.both"), isError = true)
            return
        }
        if (artifact.platform != device.platform) {
            setInstallStatus(PluginBundle.get("flutter.installer.platform.mismatch"), isError = true)
            return
        }
        if (artifact.isAppBundle && device.platform == FlutterArtifactPlatform.IOS && !device.isSimulator) {
            setInstallStatus(PluginBundle.get("flutter.installer.ipa.required"), isError = true)
            return
        }

        installing = true
        outputText = ""
        setInstallStatus(PluginBundle.get("flutter.installer.installing"))

        scope.launch {
            try {
                val result = runInterruptible(Dispatchers.IO) {
                    FlutterInstallerSupport.install(project, artifact, device)
                }
                outputText = buildString {
                    if (result.stdout.isNotBlank()) append(result.stdout.trim())
                    if (result.stderr.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(result.stderr.trim())
                    }
                }
                setInstallStatus(
                    if (result.exitCode == 0) PluginBundle.get("flutter.installer.success")
                    else PluginBundle.get("flutter.installer.failed", result.exitCode),
                    isError = result.exitCode != 0,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                setInstallStatus(e.message ?: PluginBundle.get("flutter.installer.failed.unknown"), isError = true)
                outputText = e.localizedMessage ?: e.toString()
            } finally {
                installing = false
            }
        }
    }

    // Scan once as soon as the dialog content is composed.
    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FormRow(PluginBundle.get("flutter.installer.artifact")) {
            ListComboBox(
                items = artifacts.map { it.toString() },
                selectedIndex = artifactIndex,
                onSelectedItemChange = { artifactIndex = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !installing,
            )
        }
        FormRow(PluginBundle.get("flutter.installer.device")) {
            ListComboBox(
                items = devices.map { it.toString() },
                selectedIndex = deviceIndex,
                onSelectedItemChange = { deviceIndex = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !installing,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize())
                }
            }
            Text(
                text = scanStatusMessage,
                color = if (scanStatusIsError) {
                    JewelTheme.globalColors.text.error
                } else {
                    JewelTheme.globalColors.text.normal
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            OutlinedButton(onClick = ::refresh, enabled = !scanning) {
                Icon(
                    key = AllIconsKeys.Actions.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(PluginBundle.get("flutter.installer.refresh"))
            }
        }

        TextArea(
            state = outputState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            readOnly = true,
        )
        LaunchedEffect(outputText) { outputState.setTextAndPlaceCursorAtEnd(outputText) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (installing) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize())
                }
            }
            Text(
                text = installStatusMessage,
                color = if (installStatusIsError) {
                    JewelTheme.globalColors.text.error
                } else {
                    JewelTheme.globalColors.text.normal
                },
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            DefaultButton(
                onClick = ::install,
                enabled = !installing && artifactIndex >= 0 && deviceIndex >= 0,
            ) {
                Icon(
                    key = AllIconsKeys.Actions.Execute,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(PluginBundle.get("flutter.installer.install"))
            }
        }
    }
}

@Composable
private fun FormRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(120.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

class FlutterInstallerAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { FlutterInstallerDialog(it).show() }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
        e.presentation.text = PluginBundle.get("flutter.installer.action")
        e.presentation.icon = AllIcons.Actions.Execute
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
