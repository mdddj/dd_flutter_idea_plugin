package shop.itbug.fluttercheckversionx.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalTextStyle
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.colorPalette
import java.awt.Dimension
import javax.swing.JComponent
import com.intellij.openapi.components.State as IntellijState

@IntellijState(name = "FlutterXGlobalConfig", storages = [Storage("FlutterXGlobalConfig.xml")])
@Service(Service.Level.APP)
class FlutterXGlobalConfigService : SimplePersistentStateComponent<FlutterXGlobalConfigService.MyState>(MyState()) {

    class MyState : BaseState() {
        var typeInlayOnLeft by property(false)
        var quickOpenInCommand by list<QuickOpenInCommand>()
    }

    class QuickOpenInCommand : BaseState() {
        var title by string("")
        var command by string("")
    }


    companion object {
        fun getInstance() = service<FlutterXGlobalConfigService>()
    }
}

class FlutterConfigQuickOpenInCommandDialog(project: Project) : DialogWrapper(project) {
    
    private val service = FlutterXGlobalConfigService.getInstance()
    private var commands = mutableStateListOf<CommandItem>()
    
    data class CommandItem(
        var title: String = "",
        var command: String = ""
    )
    
    init {
        title = "Quick Open In Command Configuration"
        init()
        // Load existing commands
        service.state.quickOpenInCommand.forEach {
            commands.add(CommandItem(it.title ?: "", it.command ?: ""))
        }
    }
    
    override fun createCenterPanel(): JComponent {

        return JewelComposePanel(
            {
                preferredSize = Dimension(800, 550)
            }
        ) {
            val colorPalette = JewelTheme.colorPalette
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Quick Open Commands",
                            style = LocalTextStyle.current.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Configure custom commands to open files in external applications",
                            style = LocalTextStyle.current.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                color = colorPalette.gray(8)
                            )
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            commands.add(CommandItem())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            key = AllIconsKeys.General.Add,
                            contentDescription = "Add command",
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Command list
                if (commands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorPalette.gray(13))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                               key = AllIconsKeys.General.Add,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No commands configured",
                                style = LocalTextStyle.current.copy(
                                    color = colorPalette.gray(8)
                                )
                            )
                            Text(
                                "Click the + button to add a new command",
                                style = LocalTextStyle.current.copy(
                                    fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                    color = colorPalette.gray(9)
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(commands) { index, item ->
                            CommandItemView(
                                index = index,
                                item = item,
                                onUpdate = { updated ->
                                    commands[index] = updated
                                },
                                onDelete = {
                                    commands.removeAt(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    @OptIn(ExperimentalJewelApi::class)
    @Composable
    private fun CommandItemView(
        index: Int,
        item: CommandItem,
        onUpdate: (CommandItem) -> Unit,
        onDelete: () -> Unit
    ) {
        val colorPalette = JewelTheme.colorPalette
        val titleState = remember { TextFieldState(item.title) }
        val commandState = remember { TextFieldState(item.command) }
        
        // Update parent when values change
        LaunchedEffect(titleState.text, commandState.text) {
            onUpdate(CommandItem(titleState.text.toString(), commandState.text.toString()))
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colorPalette.gray(13))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorPalette.blue(4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${index + 1}",
                        style = LocalTextStyle.current.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title input
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Title",
                            style = LocalTextStyle.current.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                color = colorPalette.gray(8)
                            )
                        )
                        TextField(
                            state = titleState,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., VSCode, Sublime Text") }
                        )
                    }
                    
                    // Command input
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Command",
                            style = LocalTextStyle.current.copy(
                                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp),
                                color = colorPalette.gray(8)
                            )
                        )
                        TextField(
                            state = commandState,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., code, subl, idea") }
                        )
                    }
                }
                
                // Delete icon button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        key = AllIconsKeys.General.Remove,
                        contentDescription = "Delete command",
                        iconClass = AllIcons::class.java,
                        tint = colorPalette.red(4)
                    )
                }
            }
        }
    }
    
    override fun doOKAction() {
        // Save commands to service
        service.state.quickOpenInCommand.clear()
        commands.forEach {
            val cmd = FlutterXGlobalConfigService.QuickOpenInCommand()
            cmd.title = it.title
            cmd.command = it.command
            service.state.quickOpenInCommand.add(cmd)
        }
        super.doOKAction()
    }
}