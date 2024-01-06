package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLKeyValue
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.util.ApiService
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName
import shop.itbug.fluttercheckversionx.util.YamlExtends
import javax.swing.Icon


/**
 * 插件新版本检测
 */
class DartPluginVersionCheck : ExternalAnnotator<DartPluginVersionCheck.Input, List<DartPluginVersionCheck.Problem>>() {

    data class Input(val file: PsiFile, val element: List<PackageInfo>)
    data class PackageInfo(val element: PsiElement, val packageInfo: DartPluginVersionName)
    data class Problem(
        val textRange: TextRange,
        val model: PubVersionDataModel,
        val element: PsiElement,
        val lastVersion: String
    )

    override fun collectInformation(file: PsiFile): Input {
        val elements = mutableListOf<PackageInfo>()
        file.originalElement.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                val ext = YamlExtends(element)
                val info = ext.getDartPluginNameAndVersion()
                val igSetting = DartPluginIgnoreConfig.getInstance(file.project)
                if (ext.isDartPluginElement() && igSetting.isIg(info?.name ?: "").not()) {
                    if (info != null) {
                        elements.add(PackageInfo(element, info))
                    }
                }
                super.visitElement(element)
            }
        })
        return Input(file, elements)
    }

    //执行长时间操作
    override fun doAnnotate(collectedInfo: Input?): List<Problem> {
        val arr = mutableListOf<Problem>()
        collectedInfo?.let {
            val infos: List<PubVersionDataModel?> = runBlocking {
                val tasks = it.element.map { info ->
                    val pluginName = info.packageInfo.name
                    val r: Deferred<PubVersionDataModel?> = async {
                        return@async ApiService.getPluginDetail(pluginName)
                    }
                    return@map r
                }
                return@runBlocking tasks.awaitAll()
            }
            it.element.forEach { info ->
                val packageName = info.packageInfo.name
                val find: PubVersionDataModel? = infos.find { detail -> detail?.name == packageName }
                find?.let { model ->
                    run {

                        val versionText = model.getLastVersionText(info.packageInfo)
                        if (versionText != null) {
                            arr.add(Problem(info.element.lastChild.textRange, model, info.element, versionText))  //有新版本
                        }
                    }
                }
            }

        }

        return arr
    }


    override fun apply(file: PsiFile, annotationResult: List<Problem>?, holder: AnnotationHolder) {
        annotationResult?.forEach {

            holder.newAnnotation(
                HighlightSeverity.WARNING, "${PluginBundle.get("version.tip.1")}:${it.lastVersion}"
            ).newFix(MyFix(it)).registerFix().range(it.element.lastChild).create()

        }

    }


    ///更新到最新版本
    private inner class MyFix(val problem: Problem) : IntentionAction, DumbAware, Iconable {
        val fixText = PluginBundle.get("version.tip.3") + problem.lastVersion
        override fun startInWriteAction(): Boolean {
            return false
        }

        override fun getFamilyName(): String {
            return fixText
        }

        override fun getText(): String {
            return fixText
        }

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
            return true
        }

        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            val newPsiElement = createDartPluginYamlPsiElement(
                project,
                DartPluginVersionName(problem.model.name, problem.lastVersion)
            )
            WriteCommandAction.runWriteCommandAction(project) {
                problem.element.replace(newPsiElement)
            }


        }

        override fun getIcon(flags: Int): Icon {
            return MyIcons.flutter
        }

    }

}


///创建包节点
private fun createDartPluginYamlPsiElement(project: Project, pluginInfo: DartPluginVersionName): YAMLKeyValue {
    return YAMLElementGenerator.getInstance(project).createYamlKeyValue(pluginInfo.name, pluginInfo.version)
}


