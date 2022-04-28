package shop.itbug.fluttercheckversionx.document

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import kotlinx.coroutines.*
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl
import shop.itbug.fluttercheckversionx.services.PubService
import shop.itbug.fluttercheckversionx.services.ServiceCreate
import shop.itbug.fluttercheckversionx.services.await


/**
 * pub包自动提示的文档
 */
class YamlDocument : DocumentationProvider {


    /**
     * 生成插件版本的提示
     */
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String {
        var tips: String? = null
        var pluginName = element?.firstChild?.text ?: ""
        if(element is LeafPsiElement){
            pluginName = element.parent.firstChild.text ?: ""
        }
        if(pluginName.isNotEmpty()){
            var detail: PubVersionDataModel? = null
             runBlocking{
                 val service = ServiceCreate.create(PubService::class.java)
                try {
                    detail = service.callPluginDetails(pluginName).await()
                }catch (e: Exception){
                    tips = "捕获到异常:"+e.localizedMessage
                }
            }
            if(detail!=null){
                return renderFullDoc(
                    pluginName = detail!!.name,
                    lastVersion = detail!!.latest.version,
                    githubUrl = detail!!.latest.pubspec.homepage,
                    desc = detail!!.latest.pubspec.description,
                    lastUpdate = detail!!.latest.published
                )
            }

        }
        return tips ?: "None"
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
        if(contextElement is YAMLKeyValueImpl){
            println(contextElement.elementType)
            return contextElement
        }
        return null
    }


    /**
     * 来自jb的参考: https://plugins.jetbrains.com/docs/intellij/documentation-provider.html#render-the-documentation
     */
    private fun renderFullDoc(pluginName: String,
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
        if(githubUrl!=null){
            addKeyValueSection("Homepage:",  "<a href='$githubUrl'>$githubUrl</>", sb)
        }
        addKeyValueSection("Pub.dev:",  "<a href='https://pub.dev/packages/$pluginName'>https://pub.dev/packages/$pluginName</>", sb)
        addKeyValueSection("LastUpdate:", lastUpdate, sb)
        sb.append(DocumentationMarkup.SECTIONS_END)
        sb.append("<br/>")
        sb.append("<p style='color:gray;padding: 6px;font-size: 10px;'>梁典典: 欢迎加入Flutter自学交流群:667186542</p>")
        return sb.toString()
    }

    private fun addKeyValueSection(key: String, value: String, sb: java.lang.StringBuilder) {
        sb.append(DocumentationMarkup.SECTION_HEADER_START)
        sb.append(key)
        sb.append(DocumentationMarkup.SECTION_SEPARATOR)
        sb.append("<p>")
        sb.append(value)
        sb.append(DocumentationMarkup.SECTION_END)
    }

}