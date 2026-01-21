package shop.itbug.flutterx.widget_tree

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.jewel.bridge.addComposeTab
import shop.itbug.flutterx.window.vm.FlutterAppsTabComponent
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.foundation.lazy.tree.rememberTreeState
import org.jetbrains.jewel.ui.component.LazyTree
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import vm.element.WidgetNode

class WidgetTreeWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.addComposeTab("Widget Tree") {
            WidgetTreeWindowContent(project)
        }
    }
}

@Composable
fun WidgetTreeWindowContent(project: Project) {
    FlutterAppsTabComponent(project) { app ->
        val scope = rememberCoroutineScope()
        val viewModel = remember(app) { WidgetTreeViewModel(app.vmService, scope) }
        
        WidgetTreeView(viewModel)
    }
}

@Composable
fun WidgetTreeView(viewModel: WidgetTreeViewModel) {
    val tree by viewModel.treeState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (isLoading) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                 CircularProgressIndicator()
             }
        } else if (error != null) {
             Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                 Text("Error: ${error}", color = JewelTheme.globalColors.text.error)
                 Spacer(Modifier.height(8.dp))
                 OutlinedButton(onClick = { viewModel.loadTree() }) {
                     Text("Retry")
                 }
             }
        } else if (tree != null) {
            val treeState = rememberTreeState()
            LazyTree(
                tree = tree!!,
                resourceLoader = { null },
                treeState = treeState,
                onElementClick = { element -> viewModel.selectNode(element.data) },
                onElementDoubleClick = {  } 
            ) { element -> 
                WidgetNodeRow(element.data)
            }
        } else {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                 Text("No widget tree data available.")
                 Spacer(Modifier.height(8.dp))
                 OutlinedButton(onClick = { viewModel.loadTree() }) {
                     Text("Load Tree")
                 }
             }
        }
    }
}

@Composable
fun WidgetNodeRow(node: WidgetNode) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(text = node.description ?: node.widgetRuntimeType ?: "Widget")
        if (!node.textPreview.isNullOrEmpty()) {
             Spacer(Modifier.width(8.dp))
             Text(text = node.textPreview, color = JewelTheme.globalColors.text.disabled)
        }
    }
}
