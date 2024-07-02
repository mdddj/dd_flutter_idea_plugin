package shop.itbug.fluttercheckversionx.window.sp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import kotlinx.serialization.json.Json

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
    private val jsonObject: SPResult = Json.decodeFromString(message)
    fun handle() {
        val type = jsonObject.type
        when (type) {
            KEYS -> keysHandle()
            VALUE_GET -> valueHandle()
            else -> {}
        }
    }

    private fun valueHandle() {
        val valueModel = Json.decodeFromString<SPValueModel>(jsonObject.jsonString)
        SpManagerListen.fireValue(valueModel)
    }

    //处理key
    private fun keysHandle() {
        val spKeysModel = Json.decodeFromString<SPKeysModel>(jsonObject.jsonString)
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

        fun listen(modelHandel: SPKeysHandle?, valueHandle: SPValueHandle?) {
            bus.connect().subscribe(topic, object : SpManagerListen {
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

