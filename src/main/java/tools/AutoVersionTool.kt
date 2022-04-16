package tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import fix.NewVersinFix
import model.PluginVersion
import util.CacheUtil


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
        val list = CacheUtil.getCatch()
        println(list.asMap())
        val newPlugins = mutableListOf<PluginVersion>()
        list.asMap().forEach { (_, u) ->
            run {
                newPlugins.add(u)
            }
        }
        println("缓存数量获取到:${newPlugins.size}")
        regProblem(newPlugins,file)
        super.visitFile(file)
    }


    /**
     * 问题注册器,并新增快速修复功能更
     */
    private fun regProblem(plugins: List<PluginVersion>,file: PsiFile) {
        plugins.map { plugin ->
            // 有新版本了,注册问题快捷修复
            val findElementAt = file.findElementAt(plugin.startIndex)
            findElementAt?.let {
                holder.registerProblem(
                    it,
                    "当前插件有新版本:${plugin.newVersion}",
                    ProblemHighlightType.WARNING,
                    NewVersinFix(file.findElementAt(plugin.startIndex)!!, plugin.newVersion)
                )
            }
        }
    }

}