package shop.itbug.fluttercheckversionx.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.theme.editorTabStyle
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.MyPackageGroup
import java.awt.Dimension
import javax.swing.JComponent

/**
 * 给flutter 项目中添加常用依赖第三个包
 */
@get:Composable
private val bgColor get() = if (JewelTheme.isDark) Color.Black else Color.White

//依赖的类型
private enum class PackageGroup(val displayName: String) {
    Provider("状态管理"),
    Sql("数据库"),
    Cache("缓存"),
    UI("UI组件"),
    Util("工具类")
}

private sealed class MyFlutterPackage() {
    //单个依赖
    data class Simple(val packageName: String, val type: MyPackageGroup, val group: PackageGroup) : MyFlutterPackage()

    //有一些依赖需要添加多个依赖
    data class Group(val packages: List<MyFlutterPackage>, val groupName: String) : MyFlutterPackage()
}

@Composable
fun AddPackageDialogContent(project: Project, onDismissRequest: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(setOf<MyFlutterPackage.Simple>()) }

    // 过滤依赖项
    val filteredPackages = if (searchQuery.isEmpty()) {
        flutterXDefinePackages
    } else {
        flutterXDefinePackages.filter { packageItem ->
            when (packageItem) {
                is MyFlutterPackage.Simple -> packageItem.packageName.contains(searchQuery, ignoreCase = true)
                is MyFlutterPackage.Group -> packageItem.groupName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // 按组分类
    val groupedPackages = filteredPackages.groupBy { packageItem ->
        when (packageItem) {
            is MyFlutterPackage.Simple -> packageItem.group
            is MyFlutterPackage.Group -> PackageGroup.Util // 默认分组
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {


            // 依赖列表
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedPackages.forEach { (group, packages) ->
                        item {
                            Text(
                                text = group.displayName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        itemsIndexed(packages) { i, packageItem ->
                            when (packageItem) {
                                is MyFlutterPackage.Simple -> {
                                    PackageItem(
                                        packageItem = packageItem,
                                        isSelected = selectedPackages.contains(packageItem),
                                        onSelectedChange = { selected ->
                                            selectedPackages = if (selected) {
                                                selectedPackages + packageItem
                                            } else {
                                                selectedPackages - packageItem
                                            }
                                        }
                                    )
                                }

                                is MyFlutterPackage.Group -> {
                                    // 处理组依赖项
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = packageItem.groupName,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "包含 ${packageItem.packages.size} 个依赖",
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredPackages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "未找到匹配的依赖",
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            // 底部按钮
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPackages.size} 个依赖已选择",
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("取消")
                    }

                    OutlinedButton(
                        onClick = {
                            // TODO: 实现添加依赖的逻辑
                            addDependenciesToProject(project, selectedPackages.toList())
                            onDismissRequest()
                        },
                        enabled = selectedPackages.isNotEmpty()
                    ) {
                        Text("添加依赖")
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageItem(
    packageItem: MyFlutterPackage.Simple,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onSelectedChange(!isSelected) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectedChange,
                modifier = Modifier.padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageItem.packageName,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isSelected) {
                Icon(
                    key = AllIconsKeys.General.GreenCheckmark,
                    contentDescription = "已选择",
                )
            }
        }
    }
}

// 模拟添加依赖到项目的方法
private fun addDependenciesToProject(project: Project, packages: List<MyFlutterPackage.Simple>) {
    // TODO: 实现将依赖添加到 pubspec.yaml 的逻辑
    // 这里应该：
    // 1. 读取项目的 pubspec.yaml 文件
    // 2. 解析 YAML 内容
    // 3. 添加选中的依赖项
    // 4. 保存文件
    // 5. 可能需要运行 'flutter pub get'

    // 示例代码（需要根据实际项目结构调整）：
    /*
    val pubspecFile = // 获取 pubspec.yaml 文件
    val yamlContent = pubspecFile.readText()
    val yaml = Yaml().load<Map<String, Any>>(yamlContent) as MutableMap

    packages.forEach { packageItem ->
        val dependencies = yaml.getOrDefault("dependencies", mutableMapOf<String, Any>()) as MutableMap
        dependencies[packageItem.packageName] = "any" // 或者指定版本
        yaml["dependencies"] = dependencies
    }

    // 保存更新后的 YAML 内容
    pubspecFile.writeText(Yaml().dump(yaml))

    // 刷新项目或运行 flutter pub get
    */
}

private val flutterXDefinePackages: List<MyFlutterPackage> = listOf(
    MyFlutterPackage.Simple("provider", MyPackageGroup.Dependencies, PackageGroup.Provider),
    MyFlutterPackage.Simple("riverpod", MyPackageGroup.Dependencies, PackageGroup.Provider),
    MyFlutterPackage.Simple("get_it", MyPackageGroup.Dependencies, PackageGroup.Provider),
    MyFlutterPackage.Simple("sqflite", MyPackageGroup.Dependencies, PackageGroup.Sql),
    MyFlutterPackage.Simple("floor", MyPackageGroup.Dependencies, PackageGroup.Sql),
    MyFlutterPackage.Simple("drift", MyPackageGroup.Dependencies, PackageGroup.Sql),
    MyFlutterPackage.Simple("shared_preferences", MyPackageGroup.Dependencies, PackageGroup.Cache),
    MyFlutterPackage.Simple("hive", MyPackageGroup.Dependencies, PackageGroup.Cache),
    MyFlutterPackage.Simple("path_provider", MyPackageGroup.Dependencies, PackageGroup.Util),
    MyFlutterPackage.Simple("intl", MyPackageGroup.Dependencies, PackageGroup.Util),
    MyFlutterPackage.Simple("http", MyPackageGroup.Dependencies, PackageGroup.Util),
    MyFlutterPackage.Simple("dio", MyPackageGroup.Dependencies, PackageGroup.Util),
    MyFlutterPackage.Simple("flutter_svg", MyPackageGroup.Dependencies, PackageGroup.UI),
    MyFlutterPackage.Simple("cached_network_image", MyPackageGroup.Dependencies, PackageGroup.UI),
)

@Composable
fun AddPackageDialog(project: Project, onDismissRequest: () -> Unit) {
    AddPackageDialogContent(project, onDismissRequest)
}

enum class DartPackageDialogTab {
    Search, AddedInConfig
}

//弹窗
class AddPackageDialogIdea(val project: Project) : DialogWrapper(project, true) {

    init {
        super.init()
        title = PluginBundle.get("search.pub.plugin")
    }

    override fun createCenterPanel(): JComponent {
        return JewelComposePanel({
            preferredSize = Dimension(600, 500)
        }) {
            var selectIndex by remember { mutableIntStateOf(0) }
            val tabIds by remember { mutableStateOf(DartPackageDialogTab.entries.toList()) }
            val tabs = remember(selectIndex, tabIds) {
                tabIds.mapIndexed { index, item ->
                    TabData.Default(
                        selected = selectIndex == index,
                        content = { _ ->
                            when (item) {
                                DartPackageDialogTab.Search -> Text("搜索")
                                DartPackageDialogTab.AddedInConfig -> Text("常用依赖")
                            }
                        },
                        closable = false,
                        onClick = {
                            selectIndex = index
                        }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                TabStrip(tabs, JewelTheme.editorTabStyle, modifier = Modifier.fillMaxWidth())
                when (DartPackageDialogTab.entries[selectIndex]) {
                    DartPackageDialogTab.Search -> {
                        Text("搜索插件")
                    }

                    DartPackageDialogTab.AddedInConfig -> {
                        AddPackageDialogContent(project) {

                        }
                    }
                }
            }

        }
    }


    override fun getPreferredSize(): Dimension {
        return Dimension(500, 400)
    }


}