package shop.itbug.flutterx.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project


/**
 * json 转换成 freezed 类的设置
 */
data class JsonToFreezedSettingModel(

    ///将类保存到目录路径
    var generateToPath: String = "",

    ///是否自动运行run build 命令
    var autoRunDartBuilder: Boolean = true
)

/**
 * 状态存储
 */
@State(name = "JsonToFreezedSettingModelConfig", storages = [Storage("JsonToFreezedSettingModelConfig.xml")])
@Service(Service.Level.PROJECT)
class JsonToFreezedSettingModelConfig : PersistentStateComponent<JsonToFreezedSettingModel> {
    private var stateModel = JsonToFreezedSettingModel()
    override fun getState(): JsonToFreezedSettingModel {
        return stateModel
    }

    override fun loadState(state: JsonToFreezedSettingModel) {
        stateModel = state
    }

    companion object {
        fun getInstance(project: Project): JsonToFreezedSettingModelConfig {
            return project.getService(JsonToFreezedSettingModelConfig::class.java)
        }
    }

}
