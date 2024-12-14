package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.document.Helper.Companion.addKeyValueSection
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.model.filteredDependenciesString
import shop.itbug.fluttercheckversionx.model.filteredDevDependenciesString
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.util.YamlExtends
import shop.itbug.fluttercheckversionx.util.firstChatToUpper
import shop.itbug.fluttercheckversionx.util.isDartPluginElement


/**
 * pub包自动提示的文档
 */
class YamlDocument : DocumentationProvider {

    /**
     * 生成插件版本的提示
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {
        element?.let {
            val packEle = YamlExtends(element).getMyDartPackageModel()
            if (packEle != null) {
                val packageInfo: PubVersionDataModel? =
                    ApplicationManager.getApplication().executeOnPooledThread<PubVersionDataModel?> {
                        PubService.callPluginDetails(packEle.packageName)
                    }.get()
                if (packageInfo != null) {
                    val detail = packageInfo
                    val lastTime = packageInfo.lastVersionUpdateTimeString
                    return renderFullDoc(
                        pluginName = detail.name,
                        lastVersion = detail.latest.version,
                        githubUrl = detail.latest.pubspec.homepage,
                        desc = detail.latest.pubspec.description,
                        lastUpdate = lastTime,
                        model = detail
                    )
                }
            }

        }
        return super.generateDoc(element, originalElement) ?: (element?.text?.toString() ?: "")
    }

    /**
     * 过滤文档
     */
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement != null) {
            if (contextElement is LeafPsiElement && contextElement.parent is YAMLKeyValueImpl && contextElement.parent.isDartPluginElement()) {
                return contextElement.parent
            }
            val isDartPluginElement = contextElement.isDartPluginElement()
            if (isDartPluginElement) {
                return contextElement
            }
        }
        return null
    }


    /**
     * 来自jb的参考: https://plugins.jetbrains.com/docs/intellij/documentation-provider.html#render-the-documentation
     */
    private fun renderFullDoc(
        pluginName: String,
        lastVersion: String,
        githubUrl: String?,
        desc: String,
        lastUpdate: String,
        model: PubVersionDataModel
    ): String {
        val pubspec = model.latest.pubspec
        val sb = StringBuilder()
        sb.append(DocumentationMarkup.DEFINITION_START)
        sb.append("Plugin detail")
        sb.append(DocumentationMarkup.DEFINITION_END)
        sb.append(DocumentationMarkup.CONTENT_START)
        sb.append(desc)
        sb.append(DocumentationMarkup.CONTENT_END)
        sb.append(DocumentationMarkup.SECTIONS_START)
        addKeyValueSection("Name:", "<b><strong>$pluginName</strong></b>", sb)
        addKeyValueSection("Latest Version:", lastVersion, sb)
        if (githubUrl != null) {
            addKeyValueSection("Homepage:", "<a href='$githubUrl'>Github</>", sb)
        }
        addKeyValueSection(
            "Pub.dev:",
            "<a href='https://pub.dev/packages/$pluginName'>Pub</>",
            sb
        )
        addKeyValueSection("LastUpdate:", lastUpdate, sb)
        if (pubspec.environment is Map<*, *>) {
            addKeyValueSection(
                "Environment:",
                pubspec.environment.map { "${it.key.toString().firstChatToUpper()} : ${it.value}" }
                    .joinToString(" , "),
                sb
            )
        }
        if (pubspec.filteredDependenciesString.isNotEmpty()) {
            addKeyValueSection("Dependencies", pubspec.generateDependenciesHtml(pubspec.filteredDependenciesString), sb)
        }

        if (pubspec.filteredDevDependenciesString.isNotEmpty()) {
            addKeyValueSection(
                "Dev dependencies",
                pubspec.generateDependenciesHtml(pubspec.filteredDevDependenciesString),
                sb
            )
        }

        sb.append(DocumentationMarkup.SECTIONS_END)


        //DocumentationHtmlUtil.docPopupPreferredMaxWidth}
        return HtmlChunk.html().child(
            HtmlChunk.div().attr("width", "500px").addRaw(sb.toString())
        ).toString()
    }


}
