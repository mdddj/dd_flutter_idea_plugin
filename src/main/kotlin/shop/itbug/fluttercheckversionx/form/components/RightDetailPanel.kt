package shop.itbug.fluttercheckversionx.form.components

import com.intellij.openapi.project.Project
import shop.itbug.fluttercheckversionx.bus.FlutterApiClickBus
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.socket.Request
import shop.itbug.fluttercheckversionx.tools.emptyBorder

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


