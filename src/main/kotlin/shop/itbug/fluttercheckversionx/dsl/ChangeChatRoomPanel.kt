package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import shop.itbug.fluttercheckversionx.model.ChatModel
import shop.itbug.fluttercheckversionx.socket.service.AppService

///切换房间的面板
fun changeRoomPanel () : DialogPanel {
    val rooms = service<AppService>().chatRooms
    val curr = service<AppService>().currentChatRoom
    val names = rooms.stream().map { it.name }.toList()
    val pane = panel {
        row {
            label("切换群聊")
        }
        row  {
            segmentedButton(names) { it }.bind(object : ObservableMutableProperty<String> {
                override fun set(value: String) {
                    service<AppService>().currentChatRoom = rooms.first { it.name == value }
                }

                override fun afterChange(listener: (String) -> Unit) {
                }

                override fun afterChange(listener: (String) -> Unit, parentDisposable: Disposable) {
                }

                override fun get(): String {
                    return curr?.name ?: ""
                }

            })

        }
        row {
            checkBox("设置为默认")
        }
    }
    return pane.addBorder()
}