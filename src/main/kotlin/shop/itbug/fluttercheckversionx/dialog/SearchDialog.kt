package shop.itbug.fluttercheckversionx.dialog

import WidgetTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.IncorrectOperationException
import com.jetbrains.compose.theme.typography
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil
import java.awt.Dimension
import javax.swing.JComponent


class SearchDialog(val project: Project) : DialogWrapper(project) {


    //项目所有插件
    var allPlugins = emptyList<String>()


    init {
        title = "搜索包"
        init()
        getAllPlugins()
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(600, 380)
    }


    private fun getAllPlugins(){
        allPlugins = MyPsiElementUtil.getAllPlugins(project)
    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, 600, 380)
            setContent {
                var name by remember { mutableStateOf(TextFieldValue("")) }
                var searchLoading by remember { mutableStateOf(false) }
                var results by remember { mutableStateOf(emptyList<shop.itbug.fluttercheckversionx.model.Package>()) }
                var pluginsState by remember { mutableStateOf(allPlugins) }
                WidgetTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("包搜索功能还在测试中,欢迎提出意见", modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        BrowserUtil.browse("https://github.com/mdddj/dd_flutter_idea_plugin/issues")
                                    }){
                                        Text("意见反馈")
                                    }
                                }
                                Divider()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = name,
                                        onValueChange = { v: TextFieldValue -> name = v },
                                        placeholder = { Text("输入包名搜索") }, modifier = Modifier.weight(1f).padding(8.dp)
                                    )
                                    Box(Modifier.width(12.dp))
                                    Button(onClick = {
                                        searchLoading = true
                                        val api = ServiceCreate.create<PubService>().search(name.text)
                                        try {
                                            api.execute().apply {
                                                results = body()?.packages ?: emptyList()
                                                searchLoading = false
                                            }
                                        } catch (e: Exception) {
                                            searchLoading = false
                                        }

                                    }) {
                                        if (searchLoading) {
                                            Text("搜索中")
                                        } else {
                                            Text("搜索包")
                                        }
                                    }
                                    if (searchLoading)
                                        CircularProgressIndicator()
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                ) {
                                    items(results.size) {
                                        pluginDetailView(results[it].`package`, project, onAdded = {
                                            pluginsState = MyPsiElementUtil.getAllPlugins(project)
                                        }, plugins = pluginsState)
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun pluginDetailView(pluginName: String, project: Project,onAdded: ()->Unit,plugins: List<String>) {


    var detail by mutableStateOf<PubVersionDataModel?>(null)




    fun fetchDetail() {
        if (detail == null) {
            kotlinx.coroutines.GlobalScope.launch {
                ServiceCreate.create<PubService>().callPluginDetails(pluginName).execute().apply {
                    detail = body()
                }
            }
        }
    }


    Column(modifier = Modifier.padding(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = 10.dp) {
            Column {

                    Text(pluginName, modifier = Modifier.padding(12.dp), style = typography.h6)

                if (detail != null) {
                    Text(
                        "最新版本:${detail!!.latest.version}",
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                        style = typography.body1
                    )
                }

                TextButton(onClick = {
                    val psiFile = MyPsiElementUtil.getPubSecpYamlFile(project)
                    if (psiFile != null) {
                        val qualifiedKeyInFile = YAMLUtil.getQualifiedKeyInFile(psiFile as YAMLFile, "dependencies")
                        val version = detail?.latest?.version
                        var versionText = "any"
                        if (version != null) {
                            versionText = "^$version"
                        }
                        val blockElement = YAMLElementGenerator.getInstance(project)
                            .createYamlKeyValue(pluginName, versionText)
                        val eolElement=  YAMLElementGenerator.getInstance(project).createEol()
                        WriteCommandAction.runWriteCommandAction(project) {
                            try {
                                qualifiedKeyInFile?.add(eolElement)
                                qualifiedKeyInFile?.add(blockElement)
                                onAdded.invoke()
                            } catch (_: IncorrectOperationException) {
                            }
                        }

                    }
                }, modifier = Modifier.padding(start = 12.dp), enabled = !plugins.contains(pluginName)) {
                    if(plugins.contains(pluginName)){
                        Text("已存在")
                    }else{
                        Text("导包到当前项目")
                    }
                }
            }
        }
        LaunchedEffect(pluginName) {
            fetchDetail()
        }

    }
}
