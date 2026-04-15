package shop.itbug.flutterx.common.yaml

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.flutterx.cache.YamlFileIgDartPackageCache
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.model.PubVersionDataModel
import shop.itbug.flutterx.model.getLastVersionText
import shop.itbug.flutterx.model.hasNewVersion
import shop.itbug.flutterx.services.PubChangelogService
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

    //更新日志,需要解析出来才有
    val changelog: String? = null
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

        suspend fun create(elementPointer: SmartPsiElementPointer<YAMLKeyValueImpl>): DartYamlModel? {
            // 1. 获取 project 时也要注意安全性
            val project = try { elementPointer.project } catch (_: Exception) { return null }

            return readAction {
                try {
                    // 2. 检查 element 是否还在
                    val element = elementPointer.element ?: return@readAction null
                    if (!element.isValid || project.isDisposed) return@readAction null

                    // 3. 这里的 pt 指针还是需要的，因为它是 element 的子元素
                    val pt = PsiTreeUtil.findChildOfType(element, YAMLPlainTextImpl::class.java) ?: return@readAction null

                    // 检查是否有 block (比如有些 pubspec 是 key: { version: 1.0.0 } 这种结构)
                    val hasBlock = PsiTreeUtil.findChildOfType(element, YAMLBlockMappingImpl::class.java) != null
                    if (hasBlock) return@readAction null

                    val version = element.valueText.trim()
                    if (version.isBlank()) return@readAction null
                    val name = element.keyText.trim()

                    // 创建 pt 的指针
                    val pointerManager = SmartPointerManager.getInstance(project)
                    val plainTextPointer = pointerManager.createSmartPsiElementPointer(pt)

                    // 直接复用传入的 elementPointer，不需要再 create 一次
                    DartYamlModel(name, version, tryParseDartVersionType(version), elementPointer, plainTextPointer)
                } catch (_: PsiInvalidElementAccessException) {
                    null
                }
            }
        }

        /**
         * 先请求 pub package 详情，只有确认存在新版本时才继续拉取 changelog
         */
        suspend fun fetch(element: SmartPsiElementPointer<YAMLKeyValueImpl>): DartYamlModel? {
            val model = create(element) ?: return null
            val (file, project) = readAction {
                val psi = model.element.element ?: return@readAction null
                psi.containingFile to psi.project
            } ?: return null
            val isIgnored = YamlFileIgDartPackageCache.getInstance(project).state.hasItem(file, model.name)
            if (isIgnored) return null
            val data = withContext(Dispatchers.IO) {
                PubService.callPluginDetails(model.name)
            } ?: return null

            val modelWithPackageInfo = model.copy(pubData = data)
            if (!modelWithPackageInfo.hasNewVersion()) {
                return modelWithPackageInfo
            }

            val changelog = withContext(Dispatchers.IO) {
                PubChangelogService.fetchLatestChangelog(model.name)?.formattedText
            }
            return modelWithPackageInfo.copy(changelog = changelog)
        }
    }
}
