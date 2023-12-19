package shop.itbug.fluttercheckversionx.activity

//
///**
// * 梁典典
// * 当项目打开的时候,会执行这个类的runActivity方法
// * 在这里启动一个子线程去检测项目中的pubspec.yaml文件.并执行检测新版本
// */
//class FlutterProjectOpenActivity : StartupActivity.Background, Disposable {
//
//
//    private lateinit var connect: MessageBusConnection
//    private val newDispose = Disposer.newDisposable()
//
//
//    override fun dispose() {
//        connect.disconnect()
//        println("启动资源被释放")
//    }
//
//
//    private fun onProjectClose(project: Project) {
//        project.projectClosed {
//            newDispose.dispose()
//            dispose()
//        }
//    }
//
//    fun run(project: Project) {
//        ///监听assets资源目录更改事件
//        connect = project.messageBus.connect(this)
//        connect.subscribe(VirtualFileManager.VFS_CHANGES, object :
//            BulkFileListener {
//            override fun after(events: MutableList<out VFileEvent>) {
//                super.after(events)
//                if (project.isDisposed) {
//                    return
//                }
//                val projectPath = project.basePath
//                val setting = GenerateAssetsClassConfig.getGenerateAssetsSetting()
//                if (!setting.autoListenFileChange) {
//                    return
//                }
//                if (projectPath != null) {
//                    events.forEach {
//                        it.file?.apply {
//                            checkAndAutoGenFile(projectPath, this, project)
//                        }
//                    }
//                }
//
//
//            }
//
//        })
//
//    }
//
//    /**
//     * 项目在idea中打开时执行函数
//     *
//     */
//
//    private fun checkAndAutoGenFile(projectPath: String, file: VirtualFile, project: Project) {
//        var filePath = file.canonicalPath
//        filePath = filePath?.replace("$projectPath/", "")
//        if (filePath != null) {
//            if (filePath.indexOf("assets") == 0) {
//                MyDartPsiElementUtil.autoGenerateAssetsDartClassFile(project, "assets", true)
//            }
//        }
//    }
//
//
//    init {
//        Disposer.register(newDispose, this)
//    }
//
//
//    override fun runActivity(project: Project) {
//
//        run(project)
//        onProjectClose(project)
//    }
//}