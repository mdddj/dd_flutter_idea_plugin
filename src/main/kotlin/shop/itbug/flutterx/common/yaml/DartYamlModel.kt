package shop.itbug.flutterx.common.yaml

import com.intellij.openapi.application.readAction
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.flutterx.cache.YamlFileIgDartPackageCache
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.model.PubVersionDataModel
import shop.itbug.flutterx.model.getLastVersionText
import shop.itbug.flutterx.model.hasNewVersion
import shop.itbug.flutterx.services.MyPackageGroup
import shop.itbug.flutterx.services.PubService
import shop.itbug.flutterx.util.*


/**
 * 创建一个新的
 */
fun DartYamlModel.createPsiElement() =
    MyYamlPsiElementFactory(element.project).createPlainPsiElement( "${getLastVersionText() ?: ""} ")

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
            // 在 readAction 内部进行所有 PSI 访问，避免元素在异步操作中失效
            return readAction {
                // 检查元素是否仍然有效
                if (!element.isValid) return@readAction null
                val pt = PsiTreeUtil.findChildOfType(element,YAMLPlainTextImpl::class.java) ?: return@readAction null
                val hasBlock = PsiTreeUtil.findChildOfType(element,YAMLBlockMappingImpl::class.java) != null
                if (hasBlock) return@readAction null
                
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
            val fileAndProject = readAction {
                if (!element.isValid) return@readAction null
                element.containingFile to element.project
            } ?: return null
            val (file, project) = fileAndProject
            val isIgnored = YamlFileIgDartPackageCache.getInstance(project).state.hasItem(file, model.name)
            if (isIgnored) return null
            val data = PubService.callPluginDetails(model.name) ?: return null
            return model.copy(pubData = data)
        }
    }
}