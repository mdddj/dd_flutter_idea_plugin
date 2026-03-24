package shop.itbug.flutterx.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.styling.TabStyle
import org.jetbrains.jewel.ui.theme.editorTabStyle
import org.jetbrains.jewel.ui.theme.simpleListItemStyle

/**
 * 自定义的 TabRow 容器。
 *
 * @param selectedTabIndex 当前选中的 Tab 索引。
 * @param tabs Tab 标题的列表。
 * @param modifier 应用于整个组件的 Modifier。
 * @param onTabClick 当一个 Tab 被点击时的回调，返回被点击的 Tab 索引。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun CustomTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    modifier: Modifier = Modifier,
    onTabClick: (Int) -> Unit,
    style: TabStyle = JewelTheme.editorTabStyle
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedTabIndex
            val backgroundColor =
                if (selected) JewelTheme.simpleListItemStyle.colors.backgroundSelectedActive else Color.Transparent
            val textColor =
                if (selected) JewelTheme.globalColors.text.normal else JewelTheme.globalColors.text.info

            Text(
                text = title,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(backgroundColor)
                        .clickable { onTabClick(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
