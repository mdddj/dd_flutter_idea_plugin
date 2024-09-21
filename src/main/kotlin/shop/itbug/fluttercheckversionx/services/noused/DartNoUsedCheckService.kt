package shop.itbug.fluttercheckversionx.services.noused

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.analyzer.DartServerData
import com.jetbrains.lang.dart.psi.impl.DartImportStatementImpl
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.MyDartPackage
import shop.itbug.fluttercheckversionx.services.MyPackageGroup
import shop.itbug.fluttercheckversionx.util.MyFileUtil
import shop.itbug.fluttercheckversionx.widget.DartNoUsedResultModel
import java.io.File
import java.nio.file.Path

private data class PathModel(
    val virtualFile: VirtualFile, val packageFile: VirtualFile?,
    val element: DartImportStatementImpl
)

/**
 * 扫描结果模型
 */
data class DartNoUsedCheckResultModel(
    /**
     * 本地包数量
     */
    val localPackageSize: Int,

    /**
     * 项目中所有导入节点数量
     */
    val importAllSize: Int,

    /**
     * 项目中引用第三方包的导入
     */
    val packageImportSize: Int,

    /**
     * 没有被使用的包
     */
    val noUsedPackageList: List<MyDartPackage>
)

/**
 * 获取导入的路径
 */
private fun PathModel.getPath(): String? {
    return packageFile?.path
}

/**
 * 启动开始检测包本地路径
 */
class DartNoUsedCheckServiceActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // todo 暂时去掉, 开始检查的时机不太对,导致结果不准确
        // DartNoUsedCheckService.getInstance(project).startCheck()
    }
}


/**
 * 检查项目中pubspec.yaml定义的依赖,有哪些没有在项目中使用
 *
 */
@Service(Service.Level.PROJECT)
class DartNoUsedCheckService(val project: Project) {

    private var packages: List<DartNoUsedModel> = emptyList()//全部的包
    private var usedPackages: Set<DartNoUsedModel> = hashSetOf()//已经使用的包
    private var collectImportPsiElementJobs: List<Deferred<MutableCollection<DartImportStatementImpl>?>>? = null
    private var analysisJobs: List<Deferred<PathModel>>? = null

    private var resultModel: DartNoUsedCheckResultModel? = null

    /**
     * 开始检测
     */
    fun startCheck(indicator: ProgressIndicator? = null) {
        packages = emptyList()
        runBlocking {
            getAllDartPackages(indicator)
        }
        println("本地packages:${packages.size}")
        startCheckAllDartFile(indicator)
    }

    /**
     * 获取库路径,使用协程来检测
     */
    private fun getAllDartPackages(indicator: ProgressIndicator? = null) {
        indicator?.text = "Starting"
        val roots = ProjectRootManager.getInstance(project).orderEntries().roots(OrderRootType.CLASSES).roots
        val localPackages: List<DartNoUsedModel> = runBlocking {
            return@runBlocking roots.map { root ->
                async {
                    readAction {
                        val vf = root as VirtualDirectoryImpl
                        vf.getDartNoUsedModel(
                            project
                        )
                    }
                }
            }.awaitAll()
        }.toList().filterNotNull().toHashSet().distinctBy { it.packageName }
        packages = localPackages
    }


    /**
     * 在所有的项目文件中检测导入路径
     * 收集导入的import
     */
    private fun startCheckAllDartFile(indicator: ProgressIndicator? = null) {
        val pm = PsiManager.getInstance(project)
        indicator?.text = PluginBundle.get("collect_all_import_files")
        val allImportElement: List<DartImportStatementImpl> = runBlocking(Dispatchers.EDT) {
            val allDartFiles = readAction { MyFileUtil.findAllProjectFiles(project) }//项目所有dart文件
            val jobs: List<Deferred<MutableCollection<DartImportStatementImpl>?>> = allDartFiles.map { file ->
                async {
                    indicator?.text2 = file.name
                    readAction {
                        val f = pm.findFile(file) ?: return@readAction null
                        PsiTreeUtil.findChildrenOfAnyType(
                            f, DartImportStatementImpl::class.java
                        )
                    }
                }
            }
            collectImportPsiElementJobs = jobs
            jobs.awaitAll()
        }.filterNotNull().flatten().distinctBy { runReadAction { it.text } }
        collectImportPsiElementJobs = null
        analysisImportElement(allImportElement)//开始分析
    }

    /**
     * 模态框中运行任务
     */
    fun checkUnUsedPackaged() {
        packages = emptyList()
        resultModel = null
        val task = object : com.intellij.openapi.progress.Task.Backgroundable(project, "Analyzing", true) {
            override fun run(indicator: ProgressIndicator) {
                startCheck(indicator)
            }

            override fun onCancel() {
                super.onCancel()
                println("关闭任务")
                collectImportPsiElementJobs?.forEach {
                    if (it.isActive) {
                        it.cancel()
                    }
                }
                analysisJobs?.forEach {
                    if (it.isActive) {
                        it.cancel()
                    }
                }
            }

            override fun onSuccess() {
                super.onSuccess()
                if (resultModel != null) {
                    DartNoUsedResultModel(project, resultModel!!).show()
                }


            }

        }
        task.queue()
    }


    /**
     * 分析导入用了哪个包
     */
    private fun analysisImportElement(imps: List<DartImportStatementImpl>, indicator: ProgressIndicator? = null) {
        println("开始分析:${imps.size}")


        var filterSize = imps.size
        val r = runBlocking {
            ///过滤掉 ../开头的,只要package导入的
            val finalImps = imps.filter {
                val uriText = readAction { it.uriElement.text }.removePrefix("\"").removePrefix("\'").removeSuffix("\"")
                    .removeSuffix("\'")
                uriText.startsWith("package:")
            }.toList()
            filterSize = finalImps.size

            indicator?.text = "Analyzing:${finalImps.size}"
            println("过滤掉后:${finalImps.size}")
            val jobs: List<Deferred<PathModel>> = finalImps.map {
                async {
                    val vf = readAction { it.originalElement.containingFile.virtualFile }

                    val model: PathModel = readAction {
                        val packageFile =
                            it.uriElement.navigationElement.reference?.resolve()?.containingFile?.virtualFile
                        PathModel(
                            element = it,
                            virtualFile = vf,
                            packageFile = packageFile
                        )
                    }
                    model
                }
            }
            analysisJobs = jobs
            jobs.awaitAll()
        }.filter { it.packageFile != null }
        analysisJobs = null
        usedPackages = runBlocking {
            r.map {
                async {
                    val path = it.getPath() ?: return@async null
                    packages.find { isFileInDirectory(path, it.packageDirectory) }
                }
            }.awaitAll()
        }.filterNotNull().toHashSet()

        val allPackages =
            DartPackageCheckService.getInstance(project).getPackageInfos()
                .filter { it.group == MyPackageGroup.Dependencies }
        val unUsePackage =
            allPackages.filter { !(usedPackages.map { u -> u.packageName }.contains(it.packageName)) }

        resultModel = DartNoUsedCheckResultModel(
            localPackageSize = packages.size,
            importAllSize = imps.size,
            packageImportSize = filterSize,
            noUsedPackageList = unUsePackage
        )


        showNotification(unUsePackage.joinToString(",") { it.packageName })

    }


    /**
     * 在文件浏览器中打开
     */
    fun openInBrowser(packageName: String) {
        val find = packages.find { it.packageName == packageName }
        if (find != null) {
            BrowserUtil.browse(Path.of(find.packageDirectory))
        }

    }


    /**
     * 判断文件是否在目录下
     */
    private fun isFileInDirectory(filePath: String, directoryPath: String): Boolean {
        val file = File(filePath)
        val directory = File(directoryPath)

        if (!file.exists() || !directory.exists()) {
            return false
        }

        val directoryCanonicalPath = directory.canonicalPath
        val fileCanonicalPath = file.canonicalPath

        // 提取包名（去除版本号部分），假设格式是 `包名-版本号`
        val directoryPackageName = getPackageName(directoryCanonicalPath)
        val filePackageName = getPackageName(fileCanonicalPath)

        // 比较包名，不比较版本号
        return filePackageName == directoryPackageName
    }

    // 提取路径中的包名，去掉版本号
    private fun getPackageName(path: String): String {
        val segments = path.split("/").filter { it.contains("-") }
        return segments.lastOrNull()?.substringBeforeLast("-") ?: path
    }

    private fun showNotification(msg: String) {
        NotificationsManager.getNotificationsManager().showNotification(
            Notification(
                "flutterx_check_unused_package",
                "Unused third-party packages",
                msg,
                NotificationType.INFORMATION
            ).apply {
                this.icon = MyIcons.flutter
            }, project
        )
    }

    companion object {

        fun getInstance(project: Project): DartNoUsedCheckService {
            return project.getService(DartNoUsedCheckService::class.java)
        }
    }
}