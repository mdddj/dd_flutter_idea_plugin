package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.document.Helper.Companion.addKeyValueSection
import shop.itbug.fluttercheckversionx.services.DartPackageCheckService
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
            val project = element.project
            val isPluginElement = element.isDartPluginElement()
            originalElement?.let {
                var pluginName = ""

                if (element is YAMLKeyValueImpl) {
                    pluginName = element.keyText
                }
                if (element is LeafPsiElement && element.parent is YAMLKeyValueImpl) {
                    pluginName = element.text
                }
                if (pluginName.isNotEmpty() && isPluginElement) {

                    DartPackageCheckService.getInstance(project).findPackageInfoByName(pluginName)?.let { packageInfo ->
                        val detail = packageInfo.second
                        if (detail != null) {
                            return renderFullDoc(
                                pluginName = detail.name,
                                lastVersion = detail.latest.version,
                                githubUrl = detail.latest.pubspec.homepage,
                                desc = detail.latest.pubspec.description,
                                lastUpdate = packageInfo.getLastUpdateTime()
                            )
                        }
                    }
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
        lastUpdate: String
    ): String {
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
        sb.append(DocumentationMarkup.SECTIONS_END)
        return sb.toString()
    }


}
