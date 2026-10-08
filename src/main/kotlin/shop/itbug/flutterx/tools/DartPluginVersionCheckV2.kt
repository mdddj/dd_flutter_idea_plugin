package shop.itbug.flutterx.tools

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.flutterx.common.yaml.DartYamlModel
import shop.itbug.flutterx.common.yaml.PubspecYamlFileTools
import shop.itbug.flutterx.common.yaml.createPsiElement
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.toHexString
import javax.swing.Icon

val YAML_DART_PACKAGE_INFO_KEY = Key.create<List<DartYamlModel>>("DART_PACKAGE_INFO_KEY")
val YAML_FILE_IS_FLUTTER_PROJECT = Key.create<Boolean>("DART_FILE_IS_DART")
private val PUBSPEC_INLAY_DATA_SIGNATURE_KEY = Key.create<String>("FLUTTERX_PUBSPEC_INLAY_DATA_SIGNATURE")


class DartPluginVersionCheckV2 : ExternalAnnotator<PubspecYamlFileTools, List<DartYamlModel>>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): PubspecYamlFileTools? {
        val yamlFile = file as? YAMLFile ?: return null
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

        val list = annotationResult ?: emptyList()
        list.forEach {
            val lastVersion = it.getLastVersionText()
            val ele = it.element.element
            val pt = it.plainText.element
            if (lastVersion != null && ele != null && pt != null) {
                holder.newAnnotation(
                    HighlightSeverity.WARNING, "${PluginBundle.get("version.tip.1")}:${lastVersion}"
                ).range(pt).withFix(FixNewVersionAction(it))
                    .create()

            }
        }
        val inlayDataSignature = file.getUserData(YAML_DART_PACKAGE_INFO_KEY)
            .orEmpty()
            .joinToString(separator = "\u0000") { model ->
                listOf(
                    model.name,
                    model.version,
                    model.pubData?.latest?.version.orEmpty(),
                    model.pubData?.lastVersionUpdateTimeString.orEmpty(),
                ).joinToString(separator = "\u0001")
            }
        if (file.getUserData(PUBSPEC_INLAY_DATA_SIGNATURE_KEY) != inlayDataSignature) {
            file.putUserData(PUBSPEC_INLAY_DATA_SIGNATURE_KEY, inlayDataSignature)
            DaemonCodeAnalyzer.getInstance(file.project).restart(
                file,
                "FlutterX pubspec package data changed",
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
        val nextVersion = model.createPsiElement()?.text?.trim().orEmpty()
        val colors = PreviewThemeColors.current()

        val children = mutableListOf<HtmlChunk>()
        children += HtmlChunk.div()
            .attr("style", "font-size: 18px; font-weight: 700; margin-bottom: 6px;")
            .addText(model.name)
        children += HtmlChunk.div()
            .attr("style", "color: ${colors.secondaryText}; margin-bottom: 2px;")
            .children(
                HtmlChunk.text("Current: ").bold(),
                HtmlChunk.text(model.version)
            )
        if (nextVersion.isNotEmpty()) {
            children += HtmlChunk.div()
                .attr("style", "color: ${colors.link}; margin-bottom: 8px;")
                .children(
                    HtmlChunk.text("Update to: ").bold(),
                    HtmlChunk.text(nextVersion)
                )
        }

        return IntentionPreviewInfo.Html(
            HtmlChunk.html()
                .child(
                    HtmlChunk.div()
                        .attr(
                            "style",
                            "max-width: 640px; padding: 8px; line-height: 1.45; color: ${colors.primaryText}; " +
                                "background: ${colors.panelBackground};"
                        )
                        .children(children)
                )
                .toString()
        )
    }

    override fun getFamilyName(): @IntentionFamilyName String {
        return fixText
    }

    override fun getText(): @IntentionName String {
        return fixText
    }

    override fun getIcon(flags: Int): Icon {
        return MyIcons.flutter
    }

}

private data class PreviewThemeColors(
    val panelBackground: String,
    val primaryText: String,
    val secondaryText: String,
    val link: String,
) {
    companion object {
        fun current(): PreviewThemeColors {
            return PreviewThemeColors(
                panelBackground = UIUtil.getPanelBackground().toHexString(),
                primaryText = UIUtil.getLabelForeground().toHexString(),
                secondaryText = UIUtil.getLabelDisabledForeground().toHexString(),
                link = JBUI.CurrentTheme.Link.Foreground.ENABLED.toHexString(),
            )
        }
    }
}
