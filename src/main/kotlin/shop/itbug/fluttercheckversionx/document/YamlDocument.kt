package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import org.slf4j.LoggerFactory
import shop.itbug.fluttercheckversionx.document.Helper.Companion.addKeyValueSection
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.services.await
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil


/**
 * pub包自动提示的文档
 */
class YamlDocument : DocumentationProvider, ExternalDocumentationProvider {

    private val logger = LoggerFactory.getLogger(YamlDocument::class.java)


    /**
     * 生成插件版本的提示
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {


        if (element == null) return ""
        if (element !is YAMLKeyValueImpl) return "无法获取该插件版本信息"


        val allPlugins = MyPsiElementUtil.getAllPlugins(element.project)
        val devPlugins = MyPsiElementUtil.getAllPlugins(element.project,"dev_dependencies")

        var tips: String? = null
        val pluginName = element.keyText
        if (!allPlugins.contains(pluginName) && !devPlugins.contains(pluginName)) return "无法获取该插件版本信息"


        if (pluginName.isNotEmpty()) {
            var detail: PubVersionDataModel? = null
            runBlocking {
                val service = ServiceCreate.create(PubService::class.java)
                try {
                    detail = service.callPluginDetails(pluginName).await()
                } catch (e: Exception) {
                    tips = "无法获取该插件版本信息:" + e.localizedMessage
                }
            }
            if (detail != null) {
                return renderFullDoc(
                    pluginName = detail!!.name,
                    lastVersion = detail!!.latest.version,
                    githubUrl = detail!!.latest.pubspec.homepage,
                    desc = detail!!.latest.pubspec.description,
                    lastUpdate = detail!!.latest.published
                )
            }

        }
        return tips ?: "无法获取该插件版本信息"
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

        val parentElement = contextElement?.parent?.parent?.parent
        if (parentElement is YAMLKeyValueImpl) {
            if (parentElement.keyText != "dependencies") {
                return null
            }
        }

        if (contextElement is YAMLKeyValueImpl) {

            return contextElement
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
        sb.append("Plugin detail (插件详情)")
        sb.append(DocumentationMarkup.DEFINITION_END)
        sb.append(DocumentationMarkup.CONTENT_START)
        sb.append(desc)
        sb.append(DocumentationMarkup.CONTENT_END)
        sb.append(DocumentationMarkup.SECTIONS_START)
        addKeyValueSection("Name:", "<b><strong>$pluginName</strong></b>", sb)
        addKeyValueSection("Latest Version:", lastVersion, sb)
        if (githubUrl != null) {
            addKeyValueSection("Homepage:", "<a href='$githubUrl'>$githubUrl</>", sb)
        }
        addKeyValueSection(
            "Pub.dev:",
            "<a href='https://pub.dev/packages/$pluginName'>https://pub.dev/packages/$pluginName</>",
            sb
        )
        addKeyValueSection("LastUpdate:", lastUpdate, sb)
        sb.append(DocumentationMarkup.SECTIONS_END)
        sb.append("<br/>")
        sb.append("<p style='color:gray;padding: 6px;font-size: 10px;'>梁典典: 欢迎加入Flutter自学交流群:667186542</p>")
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
