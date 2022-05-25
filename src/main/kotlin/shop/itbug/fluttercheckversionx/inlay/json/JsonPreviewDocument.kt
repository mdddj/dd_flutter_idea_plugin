package shop.itbug.fluttercheckversionx.inlay.json

import com.intellij.json.JsonElementTypes.DOUBLE_QUOTED_STRING
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import java.util.regex.Pattern


/// 预览图片
class JsonPreviewDocument: DocumentationProvider {


    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if(element.elementType == DOUBLE_QUOTED_STRING) {
            val url = element!!.text.replace("\"","")
            if(isImgUrl(url)){
                return "<div style=\"display: table-cell;background-size: contain;height: 400px;width: 500px;\"><img src='$url' style=\"display: block;\" /></div>"
            }
        }
        return super.generateDoc(element, originalElement)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if(contextElement.elementType == DOUBLE_QUOTED_STRING){
            return  contextElement
        }
        return null
    }

    private val imageUrlPattern: Pattern = Pattern
        .compile(".*?(gif|jpeg|png|jpg|bmp)")


    /**
     * 判断一个url是否为图片url
     *
     * @param url
     * @return
     */
    private fun isImgUrl(url: String?): Boolean {
        return if (url == null || url.trim { it <= ' ' }.isEmpty()) false else imageUrlPattern.matcher(url).matches()
    }


}