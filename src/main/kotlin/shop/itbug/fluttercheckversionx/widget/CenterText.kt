package shop.itbug.fluttercheckversionx.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text


@Composable
fun CenterText(text: String) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = JewelTheme.globalColors.text.info)
    }
}