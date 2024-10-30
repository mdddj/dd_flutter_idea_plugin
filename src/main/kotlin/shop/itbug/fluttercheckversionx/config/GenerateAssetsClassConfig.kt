package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

data class GenerateAssetsClassConfigModel(
    //类型
    var className: String = "Assets",
    //文件名
    var fileName: String = "assets",
    //保存路径
    var path: String = "lib",
    //不再提醒
    var dontTip: Boolean = false,
    //是否监听自动提醒
    var autoListenFileChange: Boolean = false,
    //忽略的文件名
    var igFiles: MutableList<String> = mutableListOf("test.json", "demo.png"),
    //添加文件夹前缀
    var addFolderNamePrefix: Boolean = false,
    //命名添加文件类型后缀: test.jpg => testJpg
    var addFileTypeSuffix: Boolean = false,
    //如果文件名里面有特殊字符,需要将其忽略
    var replaceTags: String = ".,-,!,@,#,$,%,^,&,*,(,),+,=,?,/,<,>,~",
    //文件名首字符大写
    var firstChatUpper: Boolean = true,
    //是否在编辑器中显示图标预览
    var showImageIconInEditor: Boolean = true


    //--------------------


)

@State(name = "DDGenerateAssetsClassConfig", storages = [Storage("DDGenerateAssetsClassConfig.xml")])
@Service(Service.Level.APP)
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

        fun getGenerateAssetsSetting(): GenerateAssetsClassConfigModel {
            return getInstance().state
        }
    }
}