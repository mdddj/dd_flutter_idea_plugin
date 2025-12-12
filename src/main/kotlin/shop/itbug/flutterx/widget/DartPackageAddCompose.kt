package shop.itbug.flutterx.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.flutterx.common.yaml.PubspecYamlFileTools
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.model.FlutterPluginType
import shop.itbug.flutterx.model.PubPackageInfo
import shop.itbug.flutterx.model.PubPackageSearchState
import shop.itbug.flutterx.model.PubSearchViewModel
import shop.itbug.flutterx.services.PubService
import shop.itbug.flutterx.util.MyFileUtil
import shop.itbug.flutterx.util.PubspecYamlElementFactory
import shop.itbug.flutterx.util.TaskRunUtil
import java.awt.Dimension
import java.net.URI
import javax.swing.Action
import javax.swing.JComponent

/**
 * 给flutter 项目中添加常用依赖第三个包
 */
@get:Composable
private val bgColor get() = if (JewelTheme.isDark) Color.Black.copy(alpha = 0.5f) else Color.White


//依赖的类型
enum class PackageGroup(val displayName: String) {
    Provider("State management"),
    Sql("Sql"),
    Cache("Cache"),
    UI("UI"),
    Util("Util")
}

sealed class MyFlutterPackage {
    //单个依赖
    data class Simple(val packageName: String, val type: FlutterPluginType, val group: PackageGroup) :
        MyFlutterPackage()

    data class Name(val name: String, val type: FlutterPluginType)

    //有一些依赖需要添加多个依赖
    data class Group(val packages: List<Name>, val groupName: String, val group: PackageGroup) : MyFlutterPackage()
}


@Composable
fun AddPackageDialogContent(project: Project, viewModel: PubSearchViewModel) {

    val selectIndex = viewModel.guessTabSelectIndex.collectAsState().value

    val groupedPackages: Map<PackageGroup, List<MyFlutterPackage>> = flutterXDefinePackages.groupBy { packageItem ->
        when (packageItem) {
            is MyFlutterPackage.Group -> packageItem.group
            is MyFlutterPackage.Simple -> packageItem.group
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CustomTabRow(
            selectedTabIndex = selectIndex,
            tabs = groupedPackages.keys.map { it.displayName },
            onTabClick = {
                viewModel.changeTabIndex(it)
            },
            modifier = Modifier.fillMaxWidth().background(JewelTheme.globalColors.panelBackground)
        )
        Box(modifier = Modifier.weight(1f)) {
            MyFlutterPackageListView(viewModel, groupedPackages.values.toList()[selectIndex], project)
        }
    }
}

@Composable
private fun MyFlutterPackageListView(viewModel: PubSearchViewModel, items: List<MyFlutterPackage>, project: Project) {
    val loading = viewModel.loading.collectAsState().value
    LaunchedEffect(items) {
        viewModel.getAllPackageInfo(items)
    }

    if (loading) {
        CircularProgressIndicator()
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
            itemsIndexed(items) { _, item ->
                PackageGroupView(viewModel, item, project)
            }
        }
    }

}

@Composable
private fun PackageGroupView(viewModel: PubSearchViewModel, item: MyFlutterPackage, project: Project) {
    val models = viewModel.packageGroupInfoModels.collectAsState().value
    val allDeps = viewModel.allDeps.collectAsState().value

    fun addToFile(model: PubPackageInfo, type: FlutterPluginType) {
        TaskRunUtil.runModal(project) {
            it.text = "In progress..."
            viewModel.addDepToFile(model, type)
        }
    }

    when (item) {
        is MyFlutterPackage.Group -> Box {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(item.groupName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("group", color = JewelTheme.globalColors.text.info)
                    }
                    DefaultButton({
                        item.packages.forEach {
                            val model = models[it.name]
                            if (model != null) {
                                addToFile(model, it.type)
                            }
                        }
                    }, enabled = item.packages.any { p -> !allDeps.any { it.name == p.name } }) {
                        Text("Add all")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.packages.forEach { simpleItem ->
                        val model = models[simpleItem.name]
                        if (model != null) {
                            SimplePackageItem(
                                model,
                                allDeps.any { it.name == simpleItem.name },
                                true,
                                PackageUIStype(
                                    hideDesc = true,
                                    tags = listOf(simpleItem.type.title),
                                    titleFontSize = 14
                                )
                            ) {
                                addToFile(model, simpleItem.type)
                            }
                        }
                    }
                }
            }
        }

        is MyFlutterPackage.Simple -> Box {
            val model = models[item.packageName]
            if (model != null) {
                SimplePackageItem(
                    model, allDeps.any { it.name == item.packageName }, true,
                    PackageUIStype(hideDesc = true, tags = listOf(item.type.title))
                ) {
                    addToFile(model, item.type)
                }
            }

        }
    }
}

private data class PackageUIStype(
    val hideDesc: Boolean = false,
    val tags: List<String> = emptyList(),
    val titleFontSize: Int = 18
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimplePackageItem(
    model: PubPackageInfo,
    isAdded: Boolean,
    useSimpleAddButton: Boolean = false,
    uiStyle: PackageUIStype = PackageUIStype(),
    onAdd: (type: FlutterPluginType?) -> Unit
) {
    val details = model.model
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = details.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = uiStyle.titleFontSize.sp,
                    )
                    IconButton(onClick = {
                        BrowserUtil.browse(URI.create("https://pub.dev/packages/${details.name}"))
                    }) {
                        Icon(key = AllIconsKeys.Ide.External_link_arrow, contentDescription = "")
                    }
                    Text(
                        details.latest.version,
                        color = JewelTheme.globalColors.text.info
                    )

                }
                Box(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${model.score.likeCount} likes",
                        color = JewelTheme.globalColors.text.info
                    )

                    Tooltip({ Text("Downloads within 30 days") }, enabled = true) {
                        IconText("${model.score.downloadCount30Days}", MyIcons.download)
                    }

                    Text(
                        details.formatTime(),
                        color = JewelTheme.globalColors.text.info
                    )
                    if (uiStyle.tags.isNotEmpty())
                        uiStyle.tags.forEach {
                            MiniTag(it)
                        }
                }
                if (uiStyle.hideDesc.not())
                    Column {
                        Text(
                            details.latest.pubspec.description,
                        )
                    }

            }

            Box(modifier = Modifier.width(100.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isAdded) {
                        OutlinedButton(
                            {}, enabled = false
                        ) {
                            Text("Added")
                        }
                    } else {
                        if (useSimpleAddButton) {
                            DefaultButton({
                                onAdd.invoke(null)
                            }) {
                                Text("Add")
                            }
                        } else {
                            DefaultSplitButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onAdd.invoke(FlutterPluginType.Dependencies)
                                },
                                secondaryOnClick = {
                                    println("secondary click")
                                },
                                content = { Text("Add") },
                                menuContent = {
                                    items(
                                        items = listOf(
                                            FlutterPluginType.DevDependencies,
                                            FlutterPluginType.OverridesDependencies
                                        ),
                                        isSelected = { false },
                                        onItemClick = {
                                            onAdd.invoke(it)
                                        },
                                        content = { Text(it.title) },
                                    )
                                },
                            )
                        }


                    }
                    GithubAndPub(model)
                }
            }


        }
    }

}


@Composable
private fun GithubAndPub(model: PubPackageInfo) {
    val repository = model.model.latest.pubspec.repository
    val homepage = model.model.latest.pubspec.homepage
    Box {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (repository != null && repository.isNotBlank())
                Link("repository", onClick = {
                    BrowserUtil.browse(repository)
                })
            if (homepage != null && homepage.isNotBlank())
                Link("homepage", onClick = {
                    BrowserUtil.browse(homepage)
                })
        }
    }
}

@Composable
private fun MiniTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (JewelTheme.isDark) Color.DarkGray else Color.LightGray)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = JewelTheme.globalColors.text.info, fontSize = 10.sp)
    }
}

@Composable
private fun IconText(text: String, icon: IconKey) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(key = icon, contentDescription = "", tint = JewelTheme.globalColors.text.info)
        Text(text, color = JewelTheme.globalColors.text.info)
    }
}


private val flutterXDefinePackages: List<MyFlutterPackage> = listOf(
    MyFlutterPackage.Simple("provider", FlutterPluginType.Dependencies, PackageGroup.Provider),
    MyFlutterPackage.Simple("sqflite", FlutterPluginType.Dependencies, PackageGroup.Sql),
    MyFlutterPackage.Group(
        group = PackageGroup.Sql, groupName = "isar", packages = listOf(
            MyFlutterPackage.Name("isar_community", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("isar_community_flutter_libs", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("isar_community_generator", FlutterPluginType.DevDependencies),
        )
    ),
    MyFlutterPackage.Group(
        group = PackageGroup.Cache, groupName = "hive", packages = listOf(
            MyFlutterPackage.Name("hive_ce", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("hive_ce_flutter", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("hive_ce_generator", FlutterPluginType.DevDependencies),
        )
    ),
    MyFlutterPackage.Group(
        groupName = "riverpod", group = PackageGroup.Provider, packages = listOf(
            MyFlutterPackage.Name("hooks_riverpod", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("flutter_hooks", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("riverpod_annotation", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("build_runner", FlutterPluginType.DevDependencies),
            MyFlutterPackage.Name("riverpod_generator", FlutterPluginType.DevDependencies),
            MyFlutterPackage.Name("custom_lint", FlutterPluginType.DevDependencies),
            MyFlutterPackage.Name("riverpod_lint", FlutterPluginType.DevDependencies),
        )
    ),
    MyFlutterPackage.Group(
        groupName = "freezed", group = PackageGroup.Util, packages = listOf(
            MyFlutterPackage.Name("build_runner", FlutterPluginType.DevDependencies),
            MyFlutterPackage.Name("freezed_annotation", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("freezed", FlutterPluginType.DevDependencies),
            MyFlutterPackage.Name("json_annotation", FlutterPluginType.Dependencies),
            MyFlutterPackage.Name("json_serializable", FlutterPluginType.DevDependencies)
        )
    ),
    MyFlutterPackage.Simple("cached_network_image", FlutterPluginType.Dependencies, PackageGroup.UI),
)

@Composable
fun AddPackageDialog(project: Project) {
    Column {
        OutlinedButton({
            val guessYamlFile = MyFileUtil.getPubspecFile(project) ?: return@OutlinedButton
            AddPackageDialogIdea(project, guessYamlFile).show()
        }) {
            Text("弹窗")
        }
    }
}

enum class DartPackageDialogTab {
    Search, AddedInConfig
}

//弹窗
class AddPackageDialogIdea(val project: Project, yamlFile: YAMLFile) : DialogWrapper(project, true) {

    //协程作用域
    private val scope = CoroutineScope(SupervisorJob())
    private val elementFactory = PubspecYamlElementFactory(project)
    private val yamlFileTool = PubspecYamlFileTools.create(yamlFile)

    init {
        super.init()
        title = PluginBundle.get("search.pub.plugin")
    }

    override fun createCenterPanel(): JComponent {
        val searchViewModel = PubSearchViewModel(
            viewModelScope = scope,
            pubService = PubService,
            yamlFileTool = yamlFileTool,
            elementFactory

        )
        return JewelComposePanel({
            preferredSize = Dimension(600, 500)
        }) {
            var selectIndex by remember { mutableIntStateOf(0) }
            Column(modifier = Modifier.fillMaxSize()) {
                CustomTabRow(
                    selectIndex,
                    tabs = listOf(PluginBundle.get("search"), PluginBundle.get("sugg")),
                    onTabClick = {
                        selectIndex = it
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                when (DartPackageDialogTab.entries[selectIndex]) {
                    DartPackageDialogTab.Search -> {
                        SearchPackage(project, searchViewModel)
                    }

                    DartPackageDialogTab.AddedInConfig -> {
                        AddPackageDialogContent(project, searchViewModel)
                    }
                }
            }

        }
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(500, 400)
    }

    override fun createActions(): Array<out Action?> {
        return emptyArray()
    }

}


@Composable
private fun SearchPackage(project: Project, viewModel: PubSearchViewModel) {

    val allDeps = viewModel.allDeps.collectAsState().value

    SearchCompose(viewModel) { packages ->
        val sortList = packages.sortedByDescending { it.score.likeCount }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(sortList) { _, item ->
                SimplePackageItem(item, allDeps.any { it.name == item.model.name }) { type ->
                    TaskRunUtil.runModal(project) {
                        it.text = "In progress..."
                        viewModel.addDepToFile(item, type ?: FlutterPluginType.Dependencies)
                    }
                }
            }
        }
    }
}


@Composable
private fun SearchCompose(
    viewModel: PubSearchViewModel,
    child: @Composable (result: List<PubPackageInfo>) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchStateFlow.collectAsState()
    val textFieldState = rememberTextFieldState(initialText = query)
    LaunchedEffect(textFieldState.text) {
        if (textFieldState.text.toString() != query) {
            viewModel.onQueryChanged(textFieldState.text.toString())
        }
    }
    LaunchedEffect(query) {
        delay(500)
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextField(
            state = textFieldState,
            placeholder = { Text(PluginBundle.get("search.pub.plugin")) },
            modifier = Modifier.fillMaxWidth(),
        )
        when (val state = searchState) {
            is PubPackageSearchState.Empty -> {
                Text("Enter something to start your search...")
            }

            is PubPackageSearchState.Loading -> {
                CircularProgressIndicator()
            }

            is PubPackageSearchState.Error -> {
                Text(state.error)
            }

            is PubPackageSearchState.Result -> {
                child(state.data)
            }
        }
    }
}