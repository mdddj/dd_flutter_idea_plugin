package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import shop.itbug.fluttercheckversionx.fix.NewVersinFix
import shop.itbug.fluttercheckversionx.model.FlutterPluginElementModel
import shop.itbug.fluttercheckversionx.util.ApiService
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil


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


    private val plugins = MyPsiElementUtil.getAllFlutters(holder.project)

    override fun visitFile(file: PsiFile) {
        super.visitFile(file)
        for (arr in plugins.values) {
            arr.forEach { ele ->
                regProblem(ele)
            }
        }
    }


    /**
     * 问题注册器,并新增快速修复功能更
     */
    private fun regProblem(ele: FlutterPluginElementModel) {
        val pluginDetail = ApiService.getPluginDetail(ele.name)
        val currentVersionString = ele.element.valueText
        pluginDetail?.judge(currentVersionString) {
            holder.registerProblem(
                ele.element.lastChild,
                "此插件有新版本:${it}  (更新时间:${pluginDetail.lastVersionUpdateTimeString})",
                ProblemHighlightType.WARNING,
                NewVersinFix(ele.element, it),
            )
        }
    }

}