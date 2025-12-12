package shop.itbug.flutterx.form.components

import com.intellij.openapi.project.Project
import shop.itbug.flutterx.bus.FlutterApiClickBus
import shop.itbug.flutterx.form.sub.JsonValueRender
import shop.itbug.flutterx.socket.Request
import shop.itbug.flutterx.tools.emptyBorder

/**
 * 请求详情
 * ```dart
 * print("hello world");
 * ```
 */
class RightDetailPanel(project: Project) : JsonValueRender(project) {

    init {
        border = emptyBorder()
        FlutterApiClickBus.listening {
            changeShowValue(it)
        }
    }

    private fun changeShowValue(detail: Request) {
        changeValue(detail.data)
    }

}


