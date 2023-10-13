package shop.itbug.fluttercheckversionx.hive.component

import com.alibaba.fastjson2.JSONObject
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.socket.service.DioApiService

///显示 json
class HiveValueComponent(project: Project) : BorderLayoutPanel(), DioApiService.NativeMessageProcessing {


    private val jsonEditor = JsonValueRender(p = project)

    init {
        addToCenter(jsonEditor)
        register()
    }


    ///更新值
    private fun changeJsonValue(value: Any) {
        jsonEditor.changeValue(value)
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: JSONObject?, aio: AioSession?) {
        jsonObject?.apply {
            val type = getString("type")
            if (type == "getValue" && get("data") != null) {
                changeJsonValue(get("data"))
            }
        }
    }
}