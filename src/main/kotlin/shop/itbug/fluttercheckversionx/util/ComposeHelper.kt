package shop.itbug.fluttercheckversionx.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import com.intellij.openapi.actionSystem.*
import kotlinx.coroutines.runBlocking
import vm.network.NetworkRequest
import java.util.function.Supplier
import javax.swing.JComponent

typealias ComposeDataContextProvider = suspend (component: JComponent) -> DataContext?

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.contextMenu(
    actionGroup: ActionGroup,
    place: String = ActionPlaces.TOOLWINDOW_CONTENT,
    dataContext: ComposeDataContextProvider = { DataContext.EMPTY_CONTEXT },
    onRightClickable: () -> Unit = {},
): Modifier = this.onPointerEvent(PointerEventType.Press) {
    if (it.buttons.isSecondaryPressed) {
        onRightClickable.invoke()
        val actionManager = ActionManager.getInstance()
        val popupMenu = actionManager.createActionPopupMenu(place, actionGroup)
        val awtComponent = (it.awtEventOrNull?.source as? java.awt.Component) ?: return@onPointerEvent
        (awtComponent as? JComponent?)?.let { component ->
            val ctx = runBlocking { dataContext.invoke(component) }
            if (ctx != null) {
                popupMenu.setDataContext(Supplier<DataContext> { return@Supplier ctx })
            }

        }
        val point = it.awtEventOrNull?.point ?: java.awt.Point(0, 0)
        popupMenu.component.show(awtComponent, point.x, point.y)
    }
}

fun Modifier.contextMenu(
    actionGroupId: String,
    place: String = ActionPlaces.TOOLWINDOW_CONTENT,
    dataContext: ComposeDataContextProvider = { DataContext.EMPTY_CONTEXT },
    onRightClickable: () -> Unit = {},
): Modifier = contextMenu(
    ActionManager.getInstance().getAction(actionGroupId) as DefaultActionGroup,
    place,
    dataContext,
    onRightClickable,
)

object ComposeHelper {
    val networkRequestDataKey = DataKey.create<NetworkRequest>("request")
}