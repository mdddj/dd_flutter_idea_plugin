package shop.itbug.fluttercheckversionx.hive.component

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.components.BorderLayoutPanel
import org.smartboot.socket.transport.AioSession
import shop.itbug.fluttercheckversionx.form.sub.JsonValueRender
import shop.itbug.fluttercheckversionx.socket.service.DioApiService

///显示 json
class HiveValueComponent(project: Project) : BorderLayoutPanel(), DioApiService.NativeMessageProcessing, Disposable {


    private val jsonEditor = JsonValueRender(p = project)

    init {
        addToCenter(jsonEditor)
        register()
    }


    ///更新值
    private fun changeJsonValue(value: Any?) {
        jsonEditor.changeValue(value)
    }

    override fun handleFlutterAppMessage(nativeMessage: String, jsonObject: Map<String, Any>?, aio: AioSession?) {
        val jsonString = jsonObject?.get("jsonDataString") as? String ?: return
        val obj = DioApiService.getInstance().gson.fromJson(jsonString, Map::class.java)
        val data = obj["data"] ?: return
        if (jsonObject["type"] == "getValue") {
            changeJsonValue(data)
        }
    }

    override fun dispose() {
        removeMessageProcess()
    }
}