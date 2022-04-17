package activity

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.project.stateStore
import com.intellij.psi.PsiManager
import common.YamlFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import model.PluginVersion
import notif.NotifUtils
import util.CacheUtil
import java.io.File
import javax.swing.SwingUtilities

/**
 * 梁典典
 * 当项目打开的时候,会执行这个类的runActivity方法
 * 在这里启动一个子线程去检测项目中的pubspec.yaml文件.并执行检测新版本
 */
class FlutterProjectOpenActivity : StartupActivity {


    /**
     * 项目在idea中打开时执行函数
     *
     */
    override fun runActivity(project: Project) {

        // 拿到项目中的pubspec.yaml文件,如果不存在说明不是flutter的项目,则不进行相关操作
        val pubspecYamlFile =
            LocalFileSystem.getInstance().findFileByIoFile(File("${project.stateStore.projectBasePath}/pubspec.yaml"))


        // 判断是否有插件文件
        if (pubspecYamlFile != null) {


            // idea的虚拟文件系统转换成psi文件系统,获取psi文件,psi文件可以用来遍历节点,获取定位这些信息
            val psiFile = PsiManager.getInstance(project).findFile(pubspecYamlFile)


            // 如果转换失败了,不进行任何操作
            if (psiFile != null) {


                /// 启动一个后台进程,这个是idea的开发api,直接拿来用
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking") {

                    // 在进程中执行的事件
                    override fun run(indicator: ProgressIndicator) {


                        // 清理缓存
                        CacheUtil.getCatch().asMap().clear()

                        // 这里用了kotlin的携程功能,因为要发起较多的网络请求,需要异步操作
                        val pls = runBlocking(Dispatchers.IO) {

                            // 等待携程的全部任务完成,然后将有新版本的插件Model模型接收过来
                            val ps = withContext(Dispatchers.Default) {

                                // Yaml相关操作的类
                                val yamlFileParser = YamlFileParser(psiFile)

                                // 临时变量: 项目插件依赖总数
                                var countPlugin = 0

                                // plugins : 不是最新版本的插件
                                val plugins = yamlFileParser.startCheckFile { name, index, count ->

                                    // 当开始执行插件网络请求时,会回调这个函数,来更新底部工具条的进度文本展示
                                    run {
                                        indicator.text = "梁典典:正在检测-> $name ($index/$count) 版本中..."
                                        countPlugin = count
                                    }

                                }

                                // 到了这里,说明全部的插件已经检测完毕了
                                if (plugins.isNotEmpty()) {

                                    // 全部需要更新的插件名字
                                    val pluginNames = plugins.map { it.name }

                                    // 弹出一个通知
                                    NotifUtils.showNewPluginTips(
                                        project,
                                        "一共检测${countPlugin}个插件,有${plugins.size}个插件有新版本,$pluginNames"
                                    )
                                } else {

                                    // 全部插件已经是最新的通知
                                    NotifUtils.showNewPluginTips(project, "💐恭喜!!你的Flutter第三方依赖都是最新版!!")
                                }
                                plugins
                            }
                            ps
                        }

                        // 将有新版本的插件写入缓存
                        SwingUtilities.invokeLater {
                            saveCheckResultToCatch(pls)
                        }

                    }

                    /**
                     * 保存查询结果到缓存中去
                     */
                    fun saveCheckResultToCatch(plugins: List<PluginVersion>) {
                        plugins.forEach {
                            CacheUtil.getCatch().put(it.name, it)
                        }
                    }
                })

            }

        }

    }
}