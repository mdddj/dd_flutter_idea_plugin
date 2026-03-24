package shop.itbug.flutterx.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import shop.itbug.flutterx.icons.MyIcons

@Composable
fun KofiWidget(
    modifier: Modifier = Modifier,
    url: String = "https://ko-fi.com/lxh915673"
) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                BrowserUtil.browse(url)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            key = MyIcons.kofi,
            contentDescription = "Ko-fi",
            modifier = Modifier.size(18.dp),
            tint = Color.Red
        )

        Spacer(modifier = Modifier.width(6.dp))

        Link(
            text = "Support me on Ko-fi",
            onClick = {
                BrowserUtil.browse(url)
            }
        )
    }
}