package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.impl.DartImportStatementImpl
import shop.itbug.fluttercheckversionx.common.YamlFileParser
import shop.itbug.fluttercheckversionx.fix.NewVersinFix
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.model.PluginVersion
import shop.itbug.fluttercheckversionx.util.CacheUtil


/// 忽略检测的包名
val igScanPlugin = listOf("flutter_lints");

/**
 * yaml 版本自动补全
 */
class AutoVersionTool : LocalInspectionTool() {

    /// 访问了文件
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return YamlElementVisitor(holder)
    }
}

class YamlElementVisitor(
    private val holder: ProblemsHolder
) : PsiElementVisitor() {

    override fun visitFile(file: PsiFile) {

        // 读取缓存中的需要更新的插件数据
        val list = CacheUtil.getCatch()

        //
        val newPlugins = mutableListOf<PluginVersion>()
        list.asMap().forEach { (_, u) ->
            run {
                newPlugins.add(u)
            }
        }
        regProblem(newPlugins,file)

        val allPlugins = YamlFileParser(file).allPlugins()


        CheckUnusedPackage.checkUnusedPlugin(file.project, allPlugins)

        super.visitFile(file)
    }


    /**
     * 问题注册器,并新增快速修复功能更
     */
    private fun regProblem(plugins: List<PluginVersion>, file: PsiFile) {
        plugins.map { plugin ->
            val findWithNewPositionWithFile = getPluginNameStartIndex(file, plugin.name)
            if(findWithNewPositionWithFile!=null){
                val findElementAt = file.findElementAt(findWithNewPositionWithFile.startIndex)
                findElementAt?.let {
                    holder.registerProblem(
                        it,
                        "New version:${plugin.newVersion}",
                        ProblemHighlightType.WARNING,
                        NewVersinFix(file.findElementAt(findWithNewPositionWithFile.startIndex)!!, plugin.newVersion){
                            CacheUtil.getCatch().invalidate(plugin.name)
                        }
                    )
                }
            }

        }
    }

    /**
     * 获取插件名字在编辑器中的定位
     */
    private fun getPluginNameStartIndex(file: PsiFile,pluginName: String): PluginVersion? {
        val yamlFileParser = YamlFileParser(file)
        val allPlugins = yamlFileParser.allPlugins()
        val filter = allPlugins.filter { it.name == pluginName }
        if(filter.isEmpty()){
            return null
        }
        return filter[0]
    }

    /// 检测未使用包的类
    class CheckUnusedPackage{

        companion object {

            /**
             * 检测未使用的包
             * //todo 检测未使用过的包
             */
            @OptIn(DelicateCoroutinesApi::class)
            fun checkUnusedPlugin(project: Project, allPlugins: List<PluginVersion>) {
                val files = FileTypeIndex.getFiles(DartFileType.INSTANCE, GlobalSearchScope.projectScope(project))
                // 因为有大量的文件检测工作,这里使用携程来进行优化
//                GlobalScope.launch {
//
//                    val usedArr = withContext(Dispatchers.IO) {
//                        val used = arrayListOf<String>()
//                            files.forEach {
//                                val psiTestFile =  PsiManager.getInstance(project).findFile(it)
//                                if (psiTestFile != null) {
//                                    launch {
//                                        val usedPlugins = psiFileHandle(psiTestFile)
//                                        used.addAll(usedPlugins.filter { i -> !used.contains(i) })
//                                    }
//
//                                }
//
//                            }
//                        used
//                    }
//
//                    val unused = allPlugins.filter { item -> !usedArr.contains(item.name) }
//
//
//                    val unusedNames = unused.map { it.name }
//                    if(unusedNames.isNotEmpty()){
//                        val unredCaChe = CacheUtil.unredCaChe()
//                        unredCaChe.invalidateAll()
//                        unusedNames.forEach { item ->
//                            if(!igScanPlugin.contains(item)){
//                                run {
//                                    unredCaChe.put(item, item)
//                                }
//                            }
//                        }
//                    }
//
//                }
            }

            private fun psiFileHandle(file: PsiFile): List<String> {
                val arr = arrayListOf<String>()
                    file.accept(object : PsiElementVisitor() {
                        override fun visitElement(element: PsiElement) {
                            val imports = element.children.filterIsInstance<DartImportStatementImpl>()
                            imports.forEach {
                                if (it.text.contains("package:")) {
                                    val si = it.text.indexOf("package:")
                                    val ei = it.text.indexOfFirst { c -> c == '/' }
                                    val pc = it.text.substring(si + "package:".length, ei)
                                    arr.add(pc)
                                }
                            }

                            super.visitElement(element)
                        }
                    })
                return arr
            }
        }
    }

}