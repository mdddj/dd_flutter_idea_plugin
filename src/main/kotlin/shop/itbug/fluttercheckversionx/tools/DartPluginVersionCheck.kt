package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.cache.DartPluginIgnoreConfig
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.util.DartPluginVersionName
import shop.itbug.fluttercheckversionx.util.MyYamlPsiElementFactory
import shop.itbug.fluttercheckversionx.util.YamlExtends
import javax.swing.Icon

/**
 * 插件新版本检测
 */
class DartPluginVersionCheck : ExternalAnnotator<DartPluginVersionCheck.Input, List<DartPluginVersionCheck.Problem>>(),
    DumbAware {

    data class Input(val file: PsiFile, val element: List<PackageInfo>)
    data class PackageInfo(
        val element: YAMLKeyValueImpl,
        val packageInfo: DartPluginVersionName,
        val textPsiElement: YAMLPlainTextImpl
    )

    data class Problem(
        val textRange: TextRange,
        val model: PubVersionDataModel,
        val element: PsiElement,
        val lastVersion: String,
        val textPsiElement: YAMLPlainTextImpl
    )

    override fun collectInformation(file: PsiFile): Input {
        val elements = mutableListOf<PackageInfo>()
        file.originalElement.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                val ext = YamlExtends(element)
                val info: DartPluginVersionName? = ext.getDartPluginNameAndVersion()
                if (info != null) {
                    val igSetting = DartPluginIgnoreConfig.getInstance(file.project)
                    if (igSetting.isIg(info.name).not() && !ext.isSpecifyVersion()) {
                        val lastTextElement = PsiTreeUtil.findChildOfType(element, YAMLPlainTextImpl::class.java)
                        if (lastTextElement != null) {
                            elements.add(PackageInfo(element as YAMLKeyValueImpl, info, lastTextElement))
                        }
                    }
                }
                super.visitElement(element)
            }
        })
        return Input(file, elements)
    }

    //执行长时间操作
    override fun doAnnotate(collectedInfo: Input?): List<Problem> {
        if (collectedInfo == null) return emptyList()
        val arr = mutableListOf<Problem>()
        val infos: List<PubVersionDataModel?> = runBlocking {
            val tasks = collectedInfo.element.map { info ->
                val pluginName = info.packageInfo.name
                val r: Deferred<PubVersionDataModel?> = async {
                    return@async PubService.callPluginDetails(pluginName)
                }
                return@map r
            }
            return@runBlocking tasks.awaitAll()
        }
        collectedInfo.element.forEach { info ->
            val packageName = info.packageInfo.name
            val find: PubVersionDataModel? = infos.find { detail -> detail?.name == packageName }
            find?.let { model ->
                run {
                    val versionText = model.getLastVersionText(info.packageInfo)

                    if (versionText != null) {
                        arr.add(
                            Problem(
                                info.textPsiElement.textRange,
                                model,
                                info.element,
                                versionText,
                                info.textPsiElement
                            )
                        )  //有新版本
                    }
                }
            }
        }
        return arr
    }


    override fun apply(file: PsiFile, annotationResult: List<Problem>?, holder: AnnotationHolder) {
        annotationResult?.forEach {
            val fixText = PluginBundle.get("version.tip.3") + it.lastVersion
            holder.newAnnotation(
                HighlightSeverity.WARNING, "${PluginBundle.get("version.tip.1")}:${it.lastVersion}"
            )
                .tooltip(PluginBundle.get("dart.fix.version.time.tip.title") + it.model.lastVersionUpdateTimeString)
                .newFix(object : PsiElementBaseIntentionAction(), Iconable, DumbAware {
                    override fun getFamilyName() = fixText
                    override fun getText() = fixText
                    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean =
                        element.text != it.lastVersion

                    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
                        val newElement = MyYamlPsiElementFactory.createPlainPsiElement(project, it.lastVersion)
                        if (newElement != null) {
                            it.textPsiElement.replace(newElement)
                        }
                    }


                    override fun getIcon(flags: Int): Icon = MyIcons.flutter

                }).registerFix().range(it.textRange)
                .create()

        }

    }

}
