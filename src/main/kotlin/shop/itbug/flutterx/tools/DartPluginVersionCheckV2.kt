package shop.itbug.flutterx.tools

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
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
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
private val EDITOR = Key.create<Editor>("FLUTTERX EDITOR")


class DartPluginVersionCheckV2 : ExternalAnnotator<PubspecYamlFileTools, List<DartYamlModel>>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): PubspecYamlFileTools? {
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
        val nextVersion = model.createPsiElement()?.text?.trim().orEmpty()
        val changelog = model.changelog?.trim()
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

        if (!changelog.isNullOrBlank()) {
            children += HtmlChunk.div().attr("style", "margin-top: 10px; font-size: 15px; font-weight: 700;")
                .addText("Latest changelog")
            children += HtmlChunk.div()
                .attr(
                    "style",
                    "margin-top: 6px; max-height: 420px; overflow-y: auto; padding: 10px 12px; " +
                        "border-radius: 8px; background: ${colors.surface}; border: 1px solid ${colors.border};"
                )
                .addRaw(renderChangelogHtml(changelog, nextVersion, colors))
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

    private fun renderChangelogHtml(changelog: String, nextVersion: String, colors: PreviewThemeColors): String {
        val normalizedTargetVersion = nextVersion.removePrefix("^").trim()
        val lines = changelog.lineSequence()
            .map { it.trimEnd() }
            .dropWhile { line ->
                val trimmed = line.trim()
                trimmed.isBlank() || trimmed == normalizedTargetVersion || trimmed == nextVersion
            }
            .toList()

        if (lines.isEmpty()) {
            return HtmlChunk.div().attr("style", "color: ${colors.secondaryText};").addText(changelog).toString()
        }

        return buildString {
            var index = 0
            while (index < lines.size) {
                val line = lines[index].trim()
                if (line.isBlank()) {
                    index++
                    continue
                }

                when {
                    line.startsWith(">") -> {
                        val quoted = mutableListOf<String>()
                        while (index < lines.size) {
                            val current = lines[index].trim()
                            if (!current.startsWith(">")) break
                            quoted += current.removePrefix(">").trim()
                            index++
                        }
                        append(renderQuotedBlock(quoted, colors))
                    }

                    line.startsWith("- ") -> {
                        val bullets = mutableListOf<String>()
                        while (index < lines.size) {
                            val current = lines[index].trim()
                            if (!current.startsWith("- ")) break
                            bullets += current.removePrefix("- ").trim()
                            index++
                        }
                        append(renderBulletBlock(bullets, colors))
                    }

                    else -> {
                        append(renderParagraph(line, colors))
                        index++
                    }
                }
            }
        }
    }

    private fun renderQuotedBlock(lines: List<String>, colors: PreviewThemeColors): String {
        val children = buildString {
            val bulletLines = lines.filter { it.startsWith("- ") }
            val textLines = lines.filter { !it.startsWith("- ") }

            textLines.forEach { line ->
                append(renderTextRow(line, color = colors.primaryText))
            }
            if (bulletLines.isNotEmpty()) {
                append(renderBulletBlock(bulletLines.map { it.removePrefix("- ").trim() }, colors, marginTop = 6))
            }
        }

        return HtmlChunk.div()
            .attr(
                "style",
                "margin-top: 8px; padding: 8px 10px; border-left: 3px solid ${colors.quoteBorder}; " +
                    "background: ${colors.quoteBackground}; border-radius: 6px;"
            )
            .addRaw(children)
            .toString()
    }

    private fun renderBulletBlock(items: List<String>, colors: PreviewThemeColors, marginTop: Int = 8): String {
        val itemHtml = items.joinToString("") { item ->
            HtmlChunk.tag("li")
                .attr("style", "margin: 3px 0; color: ${colors.primaryText};")
                .addText(item)
                .toString()
        }
        return HtmlChunk.tag("ul")
            .attr("style", "margin: ${marginTop}px 0 0 18px; padding: 0;")
            .addRaw(itemHtml)
            .toString()
    }

    private fun renderParagraph(text: String, colors: PreviewThemeColors): String {
        return HtmlChunk.div()
            .attr("style", "margin-top: 8px; color: ${colors.primaryText};")
            .addText(text)
            .toString()
    }

    private fun renderTextRow(text: String, color: String): String {
        return HtmlChunk.div()
            .attr("style", "margin-top: 4px; color: $color;")
            .addText(text)
            .toString()
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

private data class PreviewThemeColors(
    val panelBackground: String,
    val surface: String,
    val quoteBackground: String,
    val border: String,
    val quoteBorder: String,
    val primaryText: String,
    val secondaryText: String,
    val link: String,
) {
    companion object {
        fun current(): PreviewThemeColors {
            return PreviewThemeColors(
                panelBackground = UIUtil.getPanelBackground().toHexString(),
                surface = JBColor(0xF6F8FA, 0x313335).toHexString(),
                quoteBackground = JBColor(0xFFFFFF, 0x3A3F45).toHexString(),
                border = JBColor.border().toHexString(),
                quoteBorder = JBColor(0xCBD5E1, 0x5F6B7A).toHexString(),
                primaryText = UIUtil.getLabelForeground().toHexString(),
                secondaryText = UIUtil.getLabelDisabledForeground().toHexString(),
                link = JBUI.CurrentTheme.Link.Foreground.ENABLED.toHexString(),
            )
        }
    }
}
