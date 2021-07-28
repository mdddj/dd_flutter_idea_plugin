package common

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import fix.NewVersinFix
import kotlinx.coroutines.*
import model.PluginVersion
import model.PubVersionDataModel
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.*
import org.jetbrains.yaml.psi.impl.YAMLFileImpl
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import services.PUBL_API_URL
import services.PubService
import services.await
import kotlin.coroutines.CoroutineContext

/**
 * 处理yaml文件,处理项目使用的插件列表
 * 创建子携程来处理网络请求
 */
class YamlFileParser(
    private val file: PsiFile,
    private val hodle: ProblemsHolder
) : CoroutineScope {

    private val parentJob = SupervisorJob()
    private val pubDatas = mutableMapOf<String, PubVersionDataModel>()

    /**
     * 线程并发策略
     * Dispatchers.IO 会使用一种较高的并发策略,当要执行的代码大多数时间是在阻塞和等待中,比如执行网络请求的时候为了能支持更高的并发数量
     */
    private val scope = CoroutineScope(Dispatchers.IO + parentJob)

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext


    /**
     * 问题注册器,并新增快速修复功能更
     */
    private fun regProblem(plugins: List<PluginVersion>) {
        plugins.map { plugin ->
            // 有新版本了,注册问题快捷修复
            // 获取psielement
            val findElementAt = file.findElementAt(plugin.startIndex)!!

            hodle.registerProblem(
                findElementAt,
                "当前插件有新版本:${plugin.newVersion}",
                ProblemHighlightType.WARNING,
                NewVersinFix(file.findElementAt(plugin.startIndex)!!, plugin.newVersion)
            )
        }
    }

    /**
     * 检查文件
     */
    suspend fun startCheckFile() {
        if (isYamlFile()) {
            val allPlugins = getAllPlugins()
            println("plugins size is ${allPlugins.size}")

            println("start check plugin versions ")
            val hasNewVersionPlugins = checkVersionFormPub(allPlugins)
            regProblem(hasNewVersionPlugins)
        } else {
            print("不是yaml文件取消检测")
        }
    }

    /**
     * 从服务器中获得最新版本
     */

    private suspend fun checkVersionFormPub(plugins: List<PluginVersion>): List<PluginVersion> {
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
                            val pubData = pubService.callPluginDetails(plugin.name).await()
                            pubDatas[plugin.name] = pubData
//                      println("server version:${pubData.latest.version} <--> current:${plugin.currentVersion.replace("^","").trim()}")
                            if (pubData.latest.version.trim() != plugin.currentVersion.replace("^", "").trim()) {
                                println("${pubData.name} not last version")
                                plugin.newVersion = pubData.latest.version
                                hasNewVersionPlugins.add(plugin)

                            }
                        }else{
                            /// 如果数据存在则不重复请求
                            println("have data , no get")
                        }
                    } catch (e: Exception) {
                        println("get pub data error :${e.message}")
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