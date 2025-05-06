package shop.itbug.fluttercheckversionx.common.yaml

import com.intellij.openapi.application.readAction
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.cache.YamlFileIgDartPackageCache
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.model.hasNewVersion
import shop.itbug.fluttercheckversionx.services.MyPackageGroup
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.util.*


/**
 * 创建一个新的
 */
fun DartYamlModel.createPsiElement() =
    MyYamlPsiElementFactory.createPlainPsiElement(element.project, "${getLastVersionText() ?: ""} ")

/**
 * dart包模型(新)
 */
data class DartYamlModel(
    val name: String,
    val version: String,
    val versionType: DartVersionType,
    val element: SmartPsiElementPointer<YAMLKeyValueImpl>,
    val plainText: SmartPsiElementPointer<YAMLPlainTextImpl>,
    val pubData: PubVersionDataModel? = null,
    val type: MyPackageGroup? = null
) {

    private fun getVersionModel() = DartPluginVersionName(name, version)


    /**
     * 是否有新版本
     */
    fun hasNewVersion() = pubData?.hasNewVersion(getVersionModel()) == true

    /**
     * 获取最新版本
     */
    fun getLastVersionText() = pubData?.getLastVersionText(getVersionModel())


    /**
     * 获取介绍
     */
    fun getDesc() = (PluginBundle.get("version.tip.3") + getLastVersionText())


    /**
     * 更新时间
     */
    fun getLastUpdate() = pubData?.lastVersionUpdateTimeString

    /**
     * 获取最后更新时间
     */
    fun getLastUpdateTimeFormatString(): String {
        val time = getLastUpdate() ?: return ""
        return DateUtils.timeAgo(time)
    }

    companion object {

        suspend fun create(element: YAMLKeyValueImpl): DartYamlModel? {
            val pt = element.findChild<YAMLPlainTextImpl>() ?: return null
            val hasBlock = element.findChild<YAMLBlockMappingImpl>() != null
            if (hasBlock) return null
            return readAction {
                val version = element.valueText.trim()
                if (version.isBlank()) return@readAction null
                val name = element.keyText.trim()
                val point = SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(element)
                val plainText = SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(pt)
                DartYamlModel(
                    name, version, tryParseDartVersionType(version), point, plainText,
                )
            }
        }

        /**
         * 解析model的时候同时请求pub.dev的包数据
         * 多了一个请求
         */
        suspend fun fetch(element: YAMLKeyValueImpl): DartYamlModel? {
            val model = create(element) ?: return null
            val file = readAction { element.containingFile }
            val project = readAction { element.project }
            val isIgnored = YamlFileIgDartPackageCache.getInstance(project).state.hasItem(file, model.name)
            if (isIgnored) return null
            val data = PubService.callPluginDetails(model.name) ?: return null
            return model.copy(pubData = data)
        }
    }
}