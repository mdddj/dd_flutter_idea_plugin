package shop.itbug.fluttercheckversionx.dialog.icons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import shop.itbug.fluttercheckversionx.dialog.DownloadSource
import shop.itbug.fluttercheckversionx.dialog.DownloadSourceDialog
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.util.firstChatToUpper
import shop.itbug.fluttercheckversionx.widget.SearchResultCard


@Composable
fun MaterialIconsDialog(
    project: Project,
    onSelected: (FlutterIcon) -> Unit
) {
    FlutterIconWrapper(project, {
        MyFlutterIconsDownloadSource.materialAllSource
    }) {
        FlutterIconsPanel(
            MyFlutterIconsDownloadSource.materialDefaultImpl,
            onSelected = {
                onSelected(it)
            })
    }
}

@Composable
fun CupertinoIconsDialog(
    project: Project,
    onSelected: (FlutterIcon) -> Unit
) {
    FlutterIconWrapper(project, {
        listOf(MyFlutterIconsDownloadSource.cupertinoIconJson, MyFlutterIconsDownloadSource.cupertinoTTF)
    }) {
        FlutterIconsPanel(
            MyFlutterIconsDownloadSource.cupertinoIcon(),
            onSelected = {
                onSelected(it)
            })
    }
}

@Composable
fun FlutterIconWrapper(project: Project, task: () -> List<DownloadSource>, child: @Composable () -> Unit) {
    val downloadTask = task()
    var isAllExists by remember { mutableStateOf(downloadTask.all { it.fileIsExists() }) }
    if (isAllExists) {
        child()
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            downloadTask.forEach {
                Text(
                    "${it.description.title.firstChatToUpper()} (${it.description.size})",
                    color = JewelTheme.globalColors.text.info
                )
            }

            OutlinedButton({
                DownloadSourceDialog(project, downloadTask).showAndGet()
                isAllExists = downloadTask.all { it.fileIsExists() }
            }) {
                Text(PluginBundle.get("download_source"))
            }
        }
    }
}


/**
 * Cupertino图标展示和选择面板
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FlutterIconsPanel(
    iconBase: FlutterIconBase,
    onSelected: (FlutterIcon) -> Unit
) {
    val icons by iconBase.icons
    var searchText by remember { mutableStateOf("") }
    val textFieldState by remember { mutableStateOf(TextFieldState()) }
    var tips by remember { mutableStateOf("") }
    val gridState = rememberLazyGridState()
    val filteredIcons = remember(searchText) {
        if (searchText.isBlank()) {
            icons
        } else {
            icons.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }

    LaunchedEffect(textFieldState.text) {
        searchText = textFieldState.text.toString()
    }
    LaunchedEffect(tips){
        if(tips.isNotBlank()){
            delay(3000)
            tips = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                state = textFieldState,
                placeholder = { Text("Search") },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
           AnimatedVisibility(tips.isNotBlank()){
               Text(tips, color = JewelTheme.globalColors.text.info)
           }
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredIcons) { icon ->
                    IconItem(icon, searchText) {
                        onSelected(it)
                        tips = "[${it.name}] Copied!"
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = gridState)
            )
        }

    }
}

/**
 * 单个图标项
 */
@Composable
private fun IconItem(icon: FlutterIcon, searchQuery: String, onSelected: (FlutterIcon) -> Unit) {
    val isDark = JewelTheme.isDark
    val textColor = if (isDark) Color.White else Color.Black
    val fontFamily = icon.fontFamily
    Column(
        modifier = Modifier
            .clickable { onSelected(icon) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon.char.toString(),
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = 32.sp,
                color = textColor
            ),
            modifier = Modifier.animateContentSize()
        )
        Spacer(modifier = Modifier.height(8.dp))
        SearchResultCard(
            text = icon.name,
            searchQuery = searchQuery,
            enableFuzzyMatch = true,
            enableAnimation = true,
            color = textColor,
        )
    }
}
