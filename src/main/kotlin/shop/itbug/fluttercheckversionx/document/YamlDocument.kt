package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.slf4j.LoggerFactory
import shop.itbug.fluttercheckversionx.document.Helper.Companion.addKeyValueSection
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.util.isDartPluginElement


/**
 * pub包自动提示的文档
 */
class YamlDocument : DocumentationProvider, ExternalDocumentationProvider {

    private val logger = LoggerFactory.getLogger(YamlDocument::class.java)


    /**
     * 生成插件版本的提示
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {
        element?.let {
            originalElement?.let {

                var pluginName = ""

                if(element is YAMLKeyValueImpl && element.isDartPluginElement()){
                    pluginName = element.keyText
                }
                if(element is LeafPsiElement && element.parent is YAMLKeyValueImpl){
                    pluginName = element.text
                }
                if (pluginName.isNotEmpty()) {
                    var detail: PubVersionDataModel? = null
                    val service = ServiceCreate.create(PubService::class.java)
                    try {
                        detail = service.callPluginDetails(pluginName).execute().body()
                    } catch (_: Exception) {
                    }
                    if (detail != null) {
                        return renderFullDoc(
                            pluginName = detail.name,
                            lastVersion = detail.latest.version,
                            githubUrl = detail.latest.pubspec.homepage,
                            desc = detail.latest.pubspec.description,
                            lastUpdate = detail.latest.published
                        )
                    }

                }
            }
        }
        return  super.generateDoc(element, originalElement) ?: (element?.text?.toString() ?: "无法识别插件")
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

        if(contextElement!=null && contextElement.isDartPluginElement()){
            return  contextElement
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

    @Deprecated("Deprecated in Java")
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        logger.info("hasDocumentationFor >> ${element.elementType}")
        return false
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return true
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {
    }

}
