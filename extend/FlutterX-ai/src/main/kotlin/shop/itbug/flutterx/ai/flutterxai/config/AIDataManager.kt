package shop.itbug.flutterx.ai.flutterxai.config

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


class AICache : BaseState() {
    val models by hashSetOf<AIModel>()
}


@State(name = "FlutterX-AI", storages = [Storage("flutterx-ai.xml")])
@Service(Service.Level.APP)
class AIDataManager : SimplePersistentStateComponent<AICache>(AICache()) {

}