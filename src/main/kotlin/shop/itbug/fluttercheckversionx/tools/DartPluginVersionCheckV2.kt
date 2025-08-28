package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInsight.hints.declarative.impl.DeclarativeInlayHintsPassFactory
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.common.yaml.DartYamlModel
import shop.itbug.fluttercheckversionx.common.yaml.PubspecYamlFileTools
import shop.itbug.fluttercheckversionx.common.yaml.createPsiElement
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.icons.MyIcons
import javax.swing.Icon

val YAML_DART_PACKAGE_INFO_KEY = Key.create<List<DartYamlModel>>("DART_PACKAGE_INFO_KEY")
val YAML_FILE_IS_FLUTTER_PROJECT = Key.create<Boolean>("DART_FILE_IS_DART")
private val EDITOR = Key.create<Editor>("FLUTTERX EDITOR")


class DartPluginVersionCheckV2 : ExternalAnnotator<PubspecYamlFileTools, List<DartYamlModel>>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): PubspecYamlFileTools? {
        log().info("chllect info mation start")
        val yamlFile = file as? YAMLFile ?: return null
        file.putUserData(EDITOR, editor)
        return PubspecYamlFileTools.create(yamlFile)
    }

    override fun doAnnotate(collectedInfo: PubspecYamlFileTools?): List<DartYamlModel>? {
        collectedInfo ?: return null
        var details = runBlocking(Dispatchers.IO) { collectedInfo.getAllDependenciesList() }
        collectedInfo.file.putUserData(YAML_DART_PACKAGE_INFO_KEY, details) //数据存储到文件中
        collectedInfo.file.putUserData(YAML_FILE_IS_FLUTTER_PROJECT, runBlocking { collectedInfo.isFlutterProject() })
        details = details.filter { it.hasNewVersion() } //只返回收有新版本的
        return details
    }

    override fun apply(file: PsiFile, annotationResult: List<DartYamlModel>?, holder: AnnotationHolder) {
        log().info("apply init")
        val list = annotationResult ?: emptyList()
        log().info("有多少个插件有新版本?${list.size}")
        log().info("有多少个插件有新版本?${list.size}")
        list.forEach {
            val lastVersion = it.getLastVersionText()
            val ele = it.element.element
            val pt = it.plainText.element
            if (lastVersion != null && ele != null && pt != null) {
                holder.newAnnotation(
                    HighlightSeverity.WARNING, "${PluginBundle.get("version.tip.1")}:${lastVersion}"
                ).range(pt).withFix(FixNewVersionAction(it)).create()

            }
        }
        file.getUserData(EDITOR)?.let { editor ->
            DeclarativeInlayHintsPassFactory.scheduleRecompute(
                editor, project = file.project,
            )
        }

    }

}


//修复函数
private class FixNewVersionAction(val model: DartYamlModel) : PsiElementBaseIntentionAction(), Iconable {

    val fixText = model.getDesc()
    val lastVersion = model.getLastVersionText() ?: ""

    override fun invoke(
        project: Project, editor: Editor?, element: PsiElement
    ) {
        val createNew = model.createPsiElement() ?: return
        val ele = model.plainText.element ?: return
        ele.replace(createNew)
    }


    override fun isAvailable(
        project: Project, editor: Editor?, element: PsiElement
    ): Boolean {
        return element.text != lastVersion
    }


    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.Html(model.createPsiElement()?.text ?: "")
    }

    override fun getFamilyName(): @IntentionFamilyName String {
        return fixText
    }

    override fun getText(): @IntentionName String {
        return fixText
    }

    override fun getIcon(flags: Int): Icon? {
        return MyIcons.flutter
    }

}