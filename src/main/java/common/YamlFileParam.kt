package common

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import model.PluginVersion
import model.PubVersionDataModel
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YamlPsiElementVisitor
import org.jetbrains.yaml.psi.impl.YAMLFileImpl
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import services.PUBL_API_URL
import services.PubService
import services.await

/**
 * 开始请求数据时回调
 */
typealias CheckPluginStartCallback = (pluginName: String,index: Int,count: Int) -> Unit

/**
 * 处理yaml文件,处理项目使用的插件列表
 * 创建子携程来处理网络请求
 */
class YamlFileParser(
    private val file: PsiFile,
)  {


    private val pubDatas = mutableMapOf<String, PubVersionDataModel>()

    /**
     * 线程并发策略
     * Dispatchers.IO 会使用一种较高的并发策略,当要执行的代码大多数时间是在阻塞和等待中,比如执行网络请求的时候为了能支持更高的并发数量
     */





    /**
     * 检查文件
     */
    suspend fun startCheckFile(pluginStart: CheckPluginStartCallback? ) :  List<PluginVersion> {
        if (isYamlFile()) {
            var allPlugins = emptyList<PluginVersion>()
            runReadAction {
                 allPlugins = getAllPlugins()
            }

            println("插件总数 ${allPlugins.size}")

            println("开始检测版本是否为最新")
            val hasNewVersionPlugins = checkVersionFormPub(allPlugins,pluginStart)
            println("检测完毕,一共有${hasNewVersionPlugins.size}个插件可更新")
            return hasNewVersionPlugins

        } else {
            print("不是yaml文件取消检测")
        }
        return emptyList()
    }

    /**
     * 从服务器中获得最新版本
     */

    private suspend fun checkVersionFormPub(plugins: List<PluginVersion>,startCallback: CheckPluginStartCallback?): List<PluginVersion> {
        val build = Retrofit.Builder().baseUrl(PUBL_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val pubService = build.create(PubService::class.java)
        val hasNewVersionPlugins = mutableListOf<PluginVersion>()
        plugins.map { plugin ->

            coroutineScope {
                async {
                    try {
                        if (!pubDatas.containsKey(plugin.name)) {
                            startCallback?.let { it(plugin.name,plugins.indexOf(plugin),plugins.size) }
                            val pubData = pubService.callPluginDetails(plugin.name).await()
                            pubDatas[plugin.name] = pubData
//                      println("server version:${pubData.latest.version} <--> current:${plugin.currentVersion.replace("^","").trim()}")
                            if (pubData.latest.version.trim() != plugin.currentVersion.replace("^", "").trim()) {
                                println("${pubData.name} 有新版本 ${plugin.currentVersion} -->  ${pubData.latest.version}")
                                plugin.newVersion = pubData.latest.version
                                hasNewVersionPlugins.add(plugin)
                            }
                        }else{
                            /// 如果数据存在则不重复请求
                            println("have data , no get")
                        }
                    } catch (e: Exception) {
                        println("请求插件版本失败 :${e.message}")
                    }
                }
            }
        }.awaitAll()
        return hasNewVersionPlugins
    }


    /**
     * 通过文件来获取项目中使用的插件列表
     */
    private fun getAllPlugins(): List<PluginVersion> {

        /**
         * 全部的插件列表
         */
        val allPlugins = mutableListOf<PluginVersion>()

        val yamlFile = file as YAMLFileImpl
        val topLevelValue = yamlFile.documents[0].topLevelValue
        if (topLevelValue != null) {
            val chillers = topLevelValue.children
            if (chillers.isNotEmpty()) {
                chillers.map {
                    it.accept(object : YamlPsiElementVisitor() {
                        override fun visitKeyValue(keyValue: YAMLKeyValue) {
                            if (keyValue.keyText == "dependencies") {
                                val mappingChild = (keyValue.value as YAMLMappingImpl).children
                                mappingChild.map { it2 ->
                                    it2.accept(object : YamlPsiElementVisitor() {
                                        override fun visitKeyValue(keyValue2: YAMLKeyValue) {
//                                            println("Plugin Name: ${keyValue2.keyText} , Version : ${keyValue2.valueText}  version type is ${keyValue2.value?.javaClass.toString()}")
                                            if (keyValue2.value is YAMLPlainTextImpl) {
                                                allPlugins.add(
                                                    PluginVersion(
                                                        keyValue2.keyText,
                                                        keyValue2.valueText,
                                                        "",
                                                        keyValue2.value!!.textOffset,
                                                        keyValue2.value!!.startOffset
                                                    )
                                                )
                                            }
                                        }
                                    })
                                }

                            }
                        }
                    })
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



}