package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.util.*


///插件版本检查
class DartPluginVersionCheck : ExternalAnnotator<DartPluginVersionCheck.Input, List<DartPluginVersionCheck.Problem>>() {

    data class Input(val file: PsiFile,val element: List<PsiElement>)
    data class Problem(val textRange: TextRange,val model : PubVersionDataModel,val element: PsiElement)

    override fun collectInformation(file: PsiFile): Input {
        val elements = mutableListOf<PsiElement>()
        file.originalElement.accept(object : PsiRecursiveElementWalkingVisitor(){
            override fun visitElement(element: PsiElement) {
                val ext = YamlExtends(element)
                if(ext.isDartPluginElement() && DartPluginIgnoreConfig.getInstance(file.project).isIg(ext.getDartPluginNameAndVersion()?.name?:"").not()){
                    elements.add(element)
                }
                super.visitElement(element)
            }
        })
        return Input(file,elements)
    }




    //执行长时间操作
    override fun doAnnotate(collectedInfo: Input?): List<Problem> {
        val arr = mutableListOf<Problem>()
        collectedInfo?.let {
            val pluginElements = it.element //插件列表
            pluginElements.forEach { ele ->
                val ext = YamlExtends(ele)
                var plugin : DartPluginVersionName? = null
                runReadAction {
                     plugin = ext.getDartPluginNameAndVersion()
                }
                if(plugin!=null){
                   val model = ApiService.getPluginDetail(plugin!!.name)
                    if(model != null && model.judge(plugin!!.version){}.not()){
                        //有新版本
                        arr.add(Problem(ele.lastChild.textRange,model,ele))
                    }
                }

            }
        }
        return arr
    }


    override fun apply(file: PsiFile, annotationResult: List<Problem>?, holder: AnnotationHolder) {
        annotationResult?.forEach {

            holder.newAnnotation(HighlightSeverity.WARNING,"${PluginBundle.get("version.tip.1")}:${it.model.lastVersion}")
                .newFix(object : IntentionAction {
                    val fixText = PluginBundle.get("version.tip.3") + it.model.lastVersion
                    var available = true
                    override fun startInWriteAction(): Boolean {
                        return true
                    }

                    override fun getFamilyName(): String {
                        return fixText
                    }

                    override fun getText(): String {
                        return fixText
                    }

                    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
                        return available
                    }

                    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
                        MyPsiElementUtil.modifyPsiElementText(it.element.lastChild,it.model.lastVersion)
                        available = false
                        project.restartPubFileAnalyzer();
                    }


                }).registerFix().range(it.element.lastChild).needsUpdateOnTyping().create()

        }

    }


}