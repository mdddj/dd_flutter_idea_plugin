package shop.itbug.fluttercheckversionx.window.sp

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic

val bus = ApplicationManager.getApplication().messageBus

data class SPResult(
    val type: String,
    val jsonString: String,
    val runtimeType: String
)

data class SPKeysModel(
    val keys: List<String>,
    val runtimeType: String
)

data class SPValueModel(
    val value: Any?,
    val runtimeType: String
)


class SpManager(message: String) {

    //private val jsonObject: SPResult = JSONObject.parseObject(message, SPResult::class.java)
    private val jsonObject: SPResult = Gson().fromJson(message, SPResult::class.java)
    fun handle() {
        val type = jsonObject.type
        when (type) {
            KEYS -> keysHandle()
            VALUE_GET -> valueHandle()
            else -> {}
        }
    }

    private fun valueHandle() {
        val valueModel = Gson().fromJson(jsonObject.jsonString, SPValueModel::class.java)
        SpManagerListen.fireValue(valueModel)
    }

    //处理key
    private fun keysHandle() {
        val spKeysModel = Gson().fromJson(jsonObject.jsonString, SPKeysModel::class.java)
        SpManagerListen.fire(spKeysModel)
    }

    companion object {
        const val KEYS = "SP_KEY"
        const val VALUE_GET = "SP_GET_VALUE"
    }
}

typealias SPKeysHandle = (model: SPKeysModel) -> Unit

typealias SPValueHandle = (value: SPValueModel?) -> Unit

interface SpManagerListen {
    fun handle(model: SPKeysModel) {}

    fun handleValue(valueModel: SPValueModel?) {}

    companion object {
        val topic = Topic.create("sp-keys", SpManagerListen::class.java)

        fun fire(model: SPKeysModel) {
            bus.syncPublisher(topic).handle(model)
        }

        fun fireValue(model: SPValueModel?) {
            bus.syncPublisher(topic).handleValue(model)
        }

        fun listen(parentDisposable: Disposable, modelHandel: SPKeysHandle?, valueHandle: SPValueHandle?) {
            bus.connect(parentDisposable).subscribe(topic, object : SpManagerListen {
                override fun handle(model: SPKeysModel) {
                    modelHandel?.invoke(model)
                }

                override fun handleValue(valueModel: SPValueModel?) {
                    valueHandle?.invoke(valueModel)
                }
            })
        }
    }
}

