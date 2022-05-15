package shop.itbug.fluttercheckversionx.dialog

import WidgetTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.services.await
import java.awt.Dimension


class SearchDialog(val project: Project) : DialogWrapper(project) {


    init {
        title = "搜索包"
        init()

    }

    override fun getPreferredSize(): Dimension {
        return Dimension(500, 300)
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, 500, 300)
            setContent {
                val state = rememberLazyListState()
                var name by remember { mutableStateOf(TextFieldValue("")) }
                var results by remember { mutableStateOf(emptyList<shop.itbug.fluttercheckversionx.model.Package>()) }
                WidgetTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = name,
                                    onValueChange = { v: TextFieldValue -> name = v },
                                    placeholder = {
                                        Text("输入包名搜索")
                                    }, modifier = Modifier.weight(1f).padding(8.dp))
                                Box(Modifier.width(12.dp))
                                Button(onClick = {
                                    runBlocking {
                                        try {
                                            val result = ServiceCreate.create<PubService>().search(name.text).await()
                                            results = result.packages
                                        } catch (e: Exception) {
                                            println("搜索失败:${e.localizedMessage}")
                                        }
                                    }
                                }) {
                                    Text("搜索包")
                                }
                            }
                            LazyColumn {
                                items(results.size) {
                                    Text(results[it].`package`)
                                }
                            }
                            VerticalScrollbar(
                                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxHeight(),
                                adapter = rememberScrollbarAdapter(
                                    scrollState = state
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun contentView() {
    Text("hello world")
}
