package shop.itbug.fluttercheckversionx.dsl

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.socket.service.AppService
import kotlin.streams.toList

///切换房间的面板
fun changeRoomPanel (setCall: ()-> Unit) : DialogPanel {
    val rooms = service<AppService>().chatRooms
    val curr = service<AppService>().currentChatRoom
    val names = rooms.stream().map { it.name }.toList()
    val pane = panel {
        row {
            label(PluginBundle.get("switch.rooms"))
        }
        row  {
            segmentedButton(names) { it }.bind(object : ObservableMutableProperty<String> {

                override fun set(value: String) {
                    service<AppService>().currentChatRoom = rooms.first { it.name == value }
//                    PluginStateService.getInstance().state?.selectRoom = rooms.first()
                    setCall.invoke()
                }

                override fun afterChange(listener: (String) -> Unit) {
                }

                override fun afterChange(listener: (String) -> Unit, parentDisposable: Disposable) {
                }

                override fun get(): String {
                    return curr?.name ?: ""
                }
            })

        }.visible(names.isNotEmpty())

        //空空如也
        row {
            label(PluginBundle.get("empty")).apply {
                takeIf { names.isEmpty() }.also {
                    component.font = JBFont.small()
                    component.foreground = UIUtil.getLabelDisabledForeground()
                }
            }
        }.visible(names.isEmpty())

        //设置默认
        row {
            checkBox(PluginBundle.get("set.default"))
        }.visible(names.isNotEmpty())
    }
    return pane.addBorder()
}