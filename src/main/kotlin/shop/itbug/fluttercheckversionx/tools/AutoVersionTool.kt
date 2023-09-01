package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.util.indexing.FileBasedIndex
import shop.itbug.fluttercheckversionx.fix.NewVersionFix
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.FlutterPluginElementModel
import shop.itbug.fluttercheckversionx.util.ApiService
import shop.itbug.fluttercheckversionx.util.CacheUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil


/**
 * yaml 版本自动补全
 */
class AutoVersionTool : LocalInspectionTool() {

    /// 访问了文件
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        MyPsiElementUtil.getPubSecpYamlFile(holder.project) ?: return PsiElementVisitor.EMPTY_VISITOR //检查一下是否为flutter项目和是否为依赖文件
        return YamlElementVisitor(holder)
    }


}


///检测版本
class YamlElementVisitor(
    private val holder: ProblemsHolder
) : PsiElementVisitor() {






    private val plugins = MyPsiElementUtil.getAllFlutters(holder.project)
    private val pubFile = MyPsiElementUtil.getPubSecpYamlFile(holder.project)

    private val manager = holder.manager

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


        var cacheModel = CacheUtil.getCatch().getIfPresent(ele.name)
        if(cacheModel == null){
           cacheModel = ApiService.getPluginDetail(ele.name)
        }
        cacheModel?.let { model ->
            CacheUtil.set(ele.name,cacheModel)
            val currentVersionString = ele.element.valueText
            cacheModel.judge(currentVersionString) {
                holder.registerProblem(
                    ele.element.lastChild,
                    "${PluginBundle.get("version.tip.1")}:${it}  (${PluginBundle.get("version.tip.2")}:${cacheModel.lastVersionUpdateTimeString})",
                    ProblemHighlightType.WARNING,
                    NewVersionFix(ele.element, it,model){
                    }
                )
            }
        }
    }


    private fun re() {
    }

    private fun reIndex() {

        pubFile?.let { psiFile ->
            FileBasedIndex.getInstance().requestReindex(psiFile.virtualFile)
        }
    }

}