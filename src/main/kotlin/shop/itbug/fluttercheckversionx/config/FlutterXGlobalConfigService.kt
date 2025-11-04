package shop.itbug.fluttercheckversionx.config

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
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
                preferredSize = Dimension(700, 500)
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick Open Commands")
                    DefaultButton(
                        onClick = {
                            commands.add(CommandItem())
                        }
                    ) {
                        Text("Add")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Command list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(commands) { index, item ->
                        CommandItemView(
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
    
    @OptIn(ExperimentalJewelApi::class)
    @Composable
    private fun CommandItemView(
        item: CommandItem,
        onUpdate: (CommandItem) -> Unit,
        onDelete: () -> Unit
    ) {
        val titleState = remember { TextFieldState(item.title) }
        val commandState = remember { TextFieldState(item.command) }
        
        // Update parent when values change
        LaunchedEffect(titleState.text, commandState.text) {
            onUpdate(CommandItem(titleState.text.toString(), commandState.text.toString()))
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Title:", modifier = Modifier.width(80.dp))
                    TextField(
                        state = titleState,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter title") }
                    )
                }
                
                // Command input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Command:", modifier = Modifier.width(80.dp))
                    TextField(
                        state = commandState,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command") }
                    )
                }
            }
            
            // Delete button
            DefaultButton(
                onClick = onDelete
            ) {
                Text("Delete")
            }
        }
        
        Divider(orientation = Orientation.Horizontal)
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