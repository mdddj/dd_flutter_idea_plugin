package shop.itbug.flutterx.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
private val cardBackground get() = JewelTheme.globalColors.panelBackground

@get:Composable
private val cardBorderColor get() = JewelTheme.globalColors.borders.normal

@get:Composable
private val subtleInfoColor get() = JewelTheme.globalColors.text.info

@get:Composable
private val chipBackgroundColor: Color
    get() = if (JewelTheme.isDark) {
        JewelTheme.globalColors.outlines.focused.copy(alpha = 0.18f)
    } else {
        JewelTheme.globalColors.outlines.focused.copy(alpha = 0.1f)
    }


//依赖的类型
enum class PackageGroup(private val displayNameKey: String) {
    Provider("pub.dev.search.group.provider"),
    Sql("pub.dev.search.group.sql"),
    Cache("pub.dev.search.group.cache"),
    UI("pub.dev.search.group.ui"),
    Util("pub.dev.search.group.util");

    val displayName: String
        get() = PluginBundle.get(displayNameKey)
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

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(PluginBundle.get("pub.dev.search.recommended.title"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                PluginBundle.get("pub.dev.search.recommended.desc"),
                color = subtleInfoColor
            )
        }
        CustomTabRow(
            selectedTabIndex = selectIndex,
            tabs = groupedPackages.keys.map { it.displayName },
            onTabClick = {
                viewModel.changeTabIndex(it)
            },
            modifier = Modifier.fillMaxWidth()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
            .background(cardBackground)
            .padding(12.dp)
    ) {
        if (loading) {
            SearchStatePanel(
                title = PluginBundle.get("pub.dev.search.recommended.loading.title"),
                message = PluginBundle.get("pub.dev.search.recommended.loading.message"),
                loading = true
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items) { _, item ->
                    PackageGroupView(viewModel, item, project)
                }
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
            it.text = PluginBundle.get("pub.dev.search.action.in.progress")
            viewModel.addDepToFile(model, type)
        }
    }

    when (item) {
        is MyFlutterPackage.Group -> Box {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(item.groupName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        MiniTag(PluginBundle.get("pub.dev.search.package.count", item.packages.size))
                        MiniTag(item.group.displayName)
                    }
                    DefaultButton({
                        item.packages.forEach {
                            val model = models[it.name]
                            if (model != null) {
                                addToFile(model, it.type)
                            }
                        }
                    }, enabled = item.packages.any { p -> !allDeps.any { it.name == p.name } }) {
                        Text(PluginBundle.get("pub.dev.search.add.all"))
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
            .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
            .background(cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = details.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = uiStyle.titleFontSize.sp,
                    )
                    Text(
                        details.latest.version,
                        color = subtleInfoColor
                    )
                    Tooltip(tooltip = { Text(PluginBundle.get("pub.dev.search.open.pub.dev")) }) {
                        IconButton(onClick = {
                            BrowserUtil.browse(URI.create("https://pub.dev/packages/${details.name}"))
                        }) {
                            Icon(key = AllIconsKeys.Ide.External_link_arrow, contentDescription = "")
                        }
                    }

                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        PluginBundle.get("pub.dev.search.likes.count", model.score.likeCount),
                        color = subtleInfoColor
                    )

                    Tooltip({ Text(PluginBundle.get("pub.dev.search.downloads.30d")) }, enabled = true) {
                        IconText("${model.score.downloadCount30Days}", MyIcons.download)
                    }

                    Text(
                        details.formatTime(),
                        color = subtleInfoColor
                    )
                    if (uiStyle.tags.isNotEmpty())
                        uiStyle.tags.forEach {
                            MiniTag(it)
                        }
                }
                if (uiStyle.hideDesc.not())
                    Text(
                        details.latest.pubspec.description,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

            }

            Box(modifier = Modifier.width(132.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (isAdded) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(PluginBundle.get("pub.dev.search.added"))
                        }
                        Text(PluginBundle.get("pub.dev.search.already.in.pubspec"), color = subtleInfoColor, fontSize = 12.sp)
                    } else {
                        if (useSimpleAddButton) {
                            DefaultButton(
                                onClick = { onAdd.invoke(null) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(PluginBundle.get("add"))
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
                                content = { Text(PluginBundle.get("add")) },
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (repository != null && repository.isNotBlank())
            Link(PluginBundle.get("pub.dev.search.link.repo"), onClick = {
                BrowserUtil.browse(repository)
            })
        if (homepage != null && homepage.isNotBlank())
            Link(PluginBundle.get("pub.dev.search.link.home"), onClick = {
                BrowserUtil.browse(homepage)
            })
    }
}

@Composable
private fun MiniTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(chipBackgroundColor)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = subtleInfoColor, fontSize = 10.sp)
    }
}

@Composable
private fun IconText(text: String, icon: IconKey) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(key = icon, contentDescription = "", tint = subtleInfoColor)
        Text(text, color = subtleInfoColor)
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
            preferredSize = Dimension(760, 580)
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
        return Dimension(760, 580)
    }

    override fun createActions(): Array<out Action?> {
        return emptyArray()
    }

}


@Composable
private fun SearchPackage(project: Project, viewModel: PubSearchViewModel) {
    val allDeps = viewModel.allDeps.collectAsState().value
    val query by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchStateFlow.collectAsState()
    val textFieldState = rememberTextFieldState(initialText = query)

    LaunchedEffect(textFieldState.text) {
        val value = textFieldState.text.toString()
        if (value != query) {
            viewModel.onQueryChanged(value)
        }
    }
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(PluginBundle.get("pub.dev.search.title"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (searchState is PubPackageSearchState.Result && query.isNotBlank()) {
                    Text(
                        PluginBundle.get("pub.dev.search.results.count", (searchState as PubPackageSearchState.Result).data.size),
                        color = subtleInfoColor
                    )
                }
            }
            Text(
                PluginBundle.get("pub.dev.search.desc"),
                color = subtleInfoColor
            )
        }

        TextField(
            state = textFieldState,
            placeholder = { Text(PluginBundle.get("pub.dev.search.placeholder")) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconActionButton(
                        key = AllIconsKeys.Actions.Close,
                        contentDescription = PluginBundle.get("pub.dev.search.clear"),
                        onClick = {
                            viewModel.onQueryChanged("")
                            textFieldState.setTextAndPlaceCursorAtEnd("")
                        }
                    )
                } else {
                    Icon(key = AllIconsKeys.Actions.Search, contentDescription = PluginBundle.get("search"))
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
                .background(cardBackground)
                .padding(12.dp)
        ) {
            when (val state = searchState) {
                is PubPackageSearchState.Empty -> {
                    SearchStatePanel(
                        title = PluginBundle.get("pub.dev.search.empty.title"),
                        message = PluginBundle.get("pub.dev.search.empty.message")
                    )
                }

                is PubPackageSearchState.Loading -> {
                    SearchStatePanel(
                        title = PluginBundle.get("pub.dev.search.loading.title"),
                        message = PluginBundle.get("pub.dev.search.loading.message"),
                        loading = true
                    )
                }

                is PubPackageSearchState.Error -> {
                    SearchStatePanel(
                        title = PluginBundle.get("pub.dev.search.error.title"),
                        message = state.error,
                        isError = true
                    )
                }

                is PubPackageSearchState.Result -> {
                    val sortedResults = state.data.sortedWith(
                        compareByDescending<PubPackageInfo> { it.score.likeCount }
                            .thenByDescending { it.score.downloadCount30Days }
                    )
                    if (sortedResults.isEmpty()) {
                        SearchStatePanel(
                            title = PluginBundle.get("pub.dev.search.no.result.title"),
                            message = PluginBundle.get("pub.dev.search.no.result.message")
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(sortedResults) { _, item ->
                                SimplePackageItem(item, allDeps.any { it.name == item.model.name }) { type ->
                                    TaskRunUtil.runModal(project) {
                                        it.text = PluginBundle.get("pub.dev.search.action.in.progress")
                                        viewModel.addDepToFile(item, type ?: FlutterPluginType.Dependencies)
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

@Composable
private fun SearchStatePanel(
    title: String,
    message: String,
    loading: Boolean = false,
    isError: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (loading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Icon(
                key = if (isError) AllIconsKeys.General.Warning else AllIconsKeys.Actions.Search,
                contentDescription = title,
                tint = if (isError) JewelTheme.globalColors.text.error else subtleInfoColor
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            message,
            color = if (isError) JewelTheme.globalColors.text.error else subtleInfoColor
        )
    }
}
