package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.document.Helper.Companion.addKeyValueSection
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.util.isDartPluginElement
import java.util.concurrent.Callable


/**
 * pub包自动提示的文档
 */
class YamlDocument : DocumentationProvider {

    /**
     * 生成插件版本的提示
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {


        element?.let {
            val isPluginElement = element.isDartPluginElement()
            originalElement?.let {

                var pluginName = ""

                if (element is YAMLKeyValueImpl && isPluginElement) {
                    pluginName = element.keyText
                }
                if (element is LeafPsiElement && element.parent is YAMLKeyValueImpl) {
                    pluginName = element.text
                }
                if (pluginName.isNotEmpty()) {

                    val future = ApplicationManager.getApplication()
                        .executeOnPooledThread(Callable<PubVersionDataModel?> { PubService.callPluginDetails(pluginName) })
                    try {
                        val detail: PubVersionDataModel? = future.get()
                        if (detail != null) {
                            return renderFullDoc(
                                pluginName = detail.name,
                                lastVersion = detail.latest.version,
                                githubUrl = detail.latest.pubspec.homepage,
                                desc = detail.latest.pubspec.description,
                                lastUpdate = detail.latest.published
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }
        return super.generateDoc(element, originalElement) ?: (element?.text?.toString() ?: "无法识别插件")
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
