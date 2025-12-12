package shop.itbug.flutterx.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.SimpleTabContent
import org.jetbrains.jewel.ui.component.TabData
import org.jetbrains.jewel.ui.component.TabStrip
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.theme.editorTabStyle

/**
 * 自定义的 TabRow 容器。
 *
 * @param selectedTabIndex 当前选中的 Tab 索引。
 * @param tabs Tab 标题的列表。
 * @param modifier 应用于整个组件的 Modifier。
 * @param onTabClick 当一个 Tab 被点击时的回调，返回被点击的 Tab 索引。
 */
@OptIn(ExperimentalJewelApi::class)
@Composable
fun CustomTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    modifier: Modifier = Modifier,
    onTabClick: (Int) -> Unit,
    style: TabStyle = JewelTheme.editorTabStyle
) {
    val tabs = remember(selectedTabIndex,tabs) {
        tabs.mapIndexed { index, string ->
            TabData.Default(
                selected = index == selectedTabIndex,
                content = { SimpleTabContent(string,it) },
                closable = false,
                onClick = { onTabClick(index) }
            )
        }
    }
    TabStrip(
        tabs, style = style,
        modifier = modifier,
    )
}
