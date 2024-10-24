package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.model.getLastVersionText
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
import shop.itbug.fluttercheckversionx.services.PubPackage
import shop.itbug.fluttercheckversionx.util.MyYamlPsiElementFactory
import javax.swing.Icon

/**
 * 插件新版本检测
 */
class DartPluginVersionCheck :
    ExternalAnnotator<DartPackageCheckService?, List<PubPackage>>(),
    DumbAware {

    override fun collectInformation(file: PsiFile): DartPackageCheckService? {
        if (file.name != "pubspec.yaml") {
            return null
        }
        return DartPackageCheckService.getInstance(file.project)
    }

    //执行长时间操作
    override fun doAnnotate(service: DartPackageCheckService?): List<PubPackage> {
        if (service == null) {
            return emptyList()
        }
        val details = service.details
        if (details.isEmpty()) return emptyList()
        val hasNewVersionItems = details.filter { service.hasNew(it) }.toList()
        if (hasNewVersionItems.isEmpty()) return emptyList()
        val elements = hasNewVersionItems.filter { it.second != null }.map { PubPackage(it.first, it.second!!) }
        return elements
    }


    override fun apply(
        file: PsiFile,
        annotationResult: List<PubPackage>,
        holder: AnnotationHolder
    ) {
        annotationResult.forEach {
            val first = it.first
            val second = it.second
            val lastVersion = second?.getLastVersionText(first.getDartPluginVersionName()) ?: ""

            val fixText = PluginBundle.get("version.tip.3") + lastVersion
            holder.newAnnotation(
                HighlightSeverity.WARNING, "${PluginBundle.get("version.tip.1")}:${lastVersion}"
            )
                .tooltip(PluginBundle.get("dart.fix.version.time.tip.title") + second?.lastVersionUpdateTimeString)
                .newFix(object : PsiElementBaseIntentionAction(), Iconable, DumbAware {
                    override fun getFamilyName() = fixText
                    override fun getText() = fixText
                    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean =
                        element.text != lastVersion

                    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
                        var newElement = MyYamlPsiElementFactory.createPlainPsiElement(project, lastVersion)
                        if (newElement != null) {
                            newElement = first.versionElement.replace(newElement) as YAMLPlainTextImpl
                            first.replaced(lastVersion, newElement)
                        }
                    }

                    override fun getIcon(flags: Int): Icon = MyIcons.flutter

                }).registerFix().range(first.versionElement.textRange)
                .create()

        }

    }

}
