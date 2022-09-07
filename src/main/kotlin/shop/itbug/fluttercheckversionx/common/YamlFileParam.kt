package shop.itbug.fluttercheckversionx.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.model.PluginVersion
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YamlPsiElementVisitor
import org.jetbrains.yaml.psi.impl.YAMLFileImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import shop.itbug.fluttercheckversionx.services.PUBL_API_URL
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.await
import shop.itbug.fluttercheckversionx.util.CacheUtil
import shop.itbug.fluttercheckversionx.util.isDartPluginElement

/**
 * 开始请求数据时回调
 */
typealias CheckPluginStartCallback = (pluginName: String, index: Int, count: Int) -> Unit

/**
 * 处理yaml文件,处理项目使用的插件列表
 * 创建子携程来处理网络请求
 */
class YamlFileParser(
    private val file: PsiFile,
) : Disposable {


    /**
     * 创建一个携程的作用域,当项目别关闭时,调用[dispose]方法进行取消任务,避免发生内存泄露
     */
    private val scope = CoroutineScope(Dispatchers.IO)


    private val pubDatas = mutableMapOf<String, PubVersionDataModel>()


    /**
     * 检查文件
     */
     fun startCheckFile(pluginStart: CheckPluginStartCallback?): List<PluginVersion> {
        if (isYamlFile()) {
            val allPlugins: List<PluginVersion> = getAllPlugins()
            return checkVersionFormPub(allPlugins, pluginStart)

        }
        return emptyList()
    }

    /**
     * 从服务器中获得最新版本
     */

    private  fun checkVersionFormPub(
        plugins: List<PluginVersion>,
        startCallback: CheckPluginStartCallback?
    ): List<PluginVersion> {
        val build = Retrofit.Builder().baseUrl(PUBL_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val pubService = build.create(PubService::class.java)
        val hasNewVersionPlugins = mutableListOf<PluginVersion>()



        plugins.map { plugin ->

            scope.async {
                run {
                    try {
                        if (!pubDatas.containsKey(plugin.name)) {
                            startCallback?.let { it(plugin.name, plugins.indexOf(plugin), plugins.size) }
                            val pubData = pubService.callPluginDetails(plugin.name).await()
                            pubDatas[plugin.name] = pubData
                            if (pubData.latest.version.trim() != plugin.currentVersion.replace("^", "").trim()) {
                                plugin.newVersion = pubData.latest.version
                                hasNewVersionPlugins.add(plugin)
                                CacheUtil.getCatch().put(plugin.name, plugin)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return hasNewVersionPlugins
    }


    fun allPlugins() = getAllPlugins()

    /**
     * 通过文件来获取项目中使用的插件列表
     */
    private fun getAllPlugins(): List<PluginVersion> {

        /**
         * 全部的插件列表
         */
        val allPlugins = mutableListOf<PluginVersion>()
        val yamlFile = file as YAMLFileImpl
        runReadAction {
            val topLevelValue = yamlFile.documents[0].topLevelValue
            if (topLevelValue != null) {
                val chillers = topLevelValue.children
                if (chillers.isNotEmpty()) {
                    chillers.map {
                        it.accept(object : YamlPsiElementVisitor() {
                            override fun visitElement(element: PsiElement) {
                                if(element.isDartPluginElement() && element is YAMLKeyValue){
                                    allPlugins.add(
                                        PluginVersion(
                                            element.keyText,
                                            element.valueText,
                                            "",
                                            element.value!!.textOffset,
                                            element.value!!.startOffset
                                        )
                                    )
                                }
                                super.visitElement(element)
                            }
                        })
                    }
                }
            }

        }
        return allPlugins
    }

    /**
     * 检查是否为yaml文件
     */
    private fun isYamlFile(): Boolean {
        return file.fileType.javaClass == YAMLFileType.YML.javaClass
    }


    /**
     * 销毁携程作用域,避免发生泄露
     */
    override fun dispose() {
        scope.cancel()
    }


}