package shop.itbug.fluttercheckversionx.config

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import java.io.File

class GenerateAssetsClassConfigModel() : BaseState() {
    var className by string("Assets")
    var fileName by string("assets")
    var path by string()
    var dontTip by property(false)
    var autoListenFileChange by property(false)
    var igFiles by list<String>()
    var addFolderNamePrefix by property(false)
    var addFileTypeSuffix by property(false)
    var replaceTags by string(".,-,!,@,#,$,%,^,&,*,(,),+,=,?,/,<,>,~")
    var firstChatUpper by property(true)

    fun addIgFiles(name: String) {
        igFiles.add(name)
        incrementModificationCount()
    }

    fun removeIgFiles(name: String) {
        igFiles.remove(name)
        incrementModificationCount()
    }

    fun initProjectPath(project: Project) {
        if (path == null || path?.isBlank() == true) {
            val initPath = project.guessProjectDir()?.path + File.separator + "lib"
            path = initPath
        }

    }
}

@State(name = "DDGenerateAssetsClassConfig", storages = [Storage("DDGenerateAssetsClassConfig.xml")])
@Service(Service.Level.PROJECT)
class GenerateAssetsClassConfig() :
    SimplePersistentStateComponent<GenerateAssetsClassConfigModel>(GenerateAssetsClassConfigModel()) {

    companion object {
        fun getInstance(project: Project): GenerateAssetsClassConfig {
            val ser = project.service<GenerateAssetsClassConfig>()
            ser.state.initProjectPath(project)
            return ser
        }


        fun getGenerateAssetsSetting(project: Project): GenerateAssetsClassConfigModel {
            return getInstance(project).state
        }
    }
}