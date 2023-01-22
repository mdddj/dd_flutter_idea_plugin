package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service


data class GenerateAssetsClassConfigModel(
    //类型
    var className: String = "R",
    //文件名
    var fileName: String = "R",
    //保持路径
    var path: String = "lib",
    //不再提醒
    var dontTip: Boolean = false,
    //是否监听自动提醒
    var autoListenFileChange: Boolean = false
)

@State(name = "DDGenerateAssetsClassConfig", storages = [Storage("DDGenerateAssetsClassConfig.xml")])
class GenerateAssetsClassConfig private constructor() : PersistentStateComponent<GenerateAssetsClassConfigModel> {
    private var model = GenerateAssetsClassConfigModel()
    override fun getState(): GenerateAssetsClassConfigModel {
        return model
    }

    override fun loadState(state: GenerateAssetsClassConfigModel) {
        model = state
    }


    companion object {
        fun getInstance(): GenerateAssetsClassConfig {
            return service()
        }

        fun getGenerateAssetsSetting() : GenerateAssetsClassConfigModel {
            return getInstance().state
        }
    }
}