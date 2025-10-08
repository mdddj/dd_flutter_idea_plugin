package shop.itbug.fluttercheckversionx.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.defaultTabStyle

/**
 * 自定义的 TabRow 容器。
 *
 * @param selectedTabIndex 当前选中的 Tab 索引。
 * @param tabs Tab 标题的列表。
 * @param modifier 应用于整个组件的 Modifier。
 * @param onTabClick 当一个 Tab 被点击时的回调，返回被点击的 Tab 索引。
 */
@Composable
fun CustomTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    modifier: Modifier = Modifier,
    onTabClick: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                CustomJewelTab(
                    title = title,
                    selected = (index == selectedTabIndex),
                    onClick = { onTabClick(index) }
                )
            }
        }
    }
}

/**
 * 自定义的单个 Tab 组件。
 *
 * @param title Tab 上显示的文本。
 * @param selected 此 Tab 当前是否被选中。
 * @param onClick 点击事件回调。
 */
@Composable
private fun CustomJewelTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tabModifier = if (selected) {
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.defaultTabStyle.colors.backgroundHovered)
    } else {
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(JewelTheme.globalColors.panelBackground)
    }

    Box(
        modifier = tabModifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),

        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}