package shop.itbug.fluttercheckversionx.activity

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import shop.itbug.fluttercheckversionx.common.YamlFileParser
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfig
import shop.itbug.fluttercheckversionx.config.GenerateAssetsClassConfigModel
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.MyNotificationUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil

/**
 * 梁典典
 * 当项目打开的时候,会执行这个类的runActivity方法
 * 在这里启动一个子线程去检测项目中的pubspec.yaml文件.并执行检测新版本
 */
class FlutterProjectOpenActivity : StartupActivity,Disposable {


    /**
     * 项目在idea中打开时执行函数
     *
     */
    override fun runActivity(project: Project) {


        val psiFile = MyPsiElementUtil.getPubSecpYamlFile(project)

        // 判断是否有插件文件
        if (psiFile != null) {
          YamlFileParser(psiFile)
        }

        ///监听assets资源目录更改事件
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object :
            BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
               val projectPath = project.basePath
               val setting =  GenerateAssetsClassConfig.getGenerateAssetsSetting()
                if(!setting.autoListenFileChange){
                    return
                }
                if(projectPath!=null){
                    events.forEach {
                        it.file?.apply {
                            checkAndAutoGenFile(projectPath,this,project,setting)
                        }
                    }
                }

                super.after(events)
            }
        })

    }


    private fun checkAndAutoGenFile(projectPath: String,file: VirtualFile,project: Project,setting: GenerateAssetsClassConfigModel) {
       var filePath = file.canonicalPath
       filePath = filePath?.replace("$projectPath/","")
        if(filePath!=null){
            println(filePath)
            if(filePath.indexOf("assets") == 0) {
                println("满足自动生成文件条件,即将执行:${filePath}")
                MyDartPsiElementUtil.autoGenerateAssetsDartClassFile(project,"assets")
                MyNotificationUtil.toolWindowShowMessage(project,"自动生成资产文件成功")
            }
        }
    }

    override fun dispose() {

    }

}