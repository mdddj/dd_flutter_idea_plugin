package shop.itbug.flutterx.tools

import com.intellij.json.JsonFileType
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile

object JsonPsiFactory {


    fun createNewJsonProp(project: Project, key: String, value: String): JsonProperty {
        val file = createJsonPsiFile(
            project, """
            { "$key" : "$value" }
        """.trimIndent()
        )
        return PsiTreeUtil.findChildOfType(file, JsonProperty::class.java)!!
    }

    //创建 string节点
    fun createJsonStringLiteral(project: Project, string: String): JsonStringLiteral {
        val file = createJsonPsiFile(
            project, """
            {
                "$string" : "$string"
            }
        """.trimIndent()
        )
        return PsiTreeUtil.findChildrenOfType(file, JsonStringLiteral::class.java).first()
    }

    //创建 JSON虚拟Psifile
    fun createJsonPsiFile(project: Project, text: String): PsiFile {
        val factory = PsiFileFactory.getInstance(project)
        val name = "dummy." + JsonFileType.INSTANCE.defaultExtension
        val lvf = LightVirtualFile(name, JsonFileType.INSTANCE, text)
        val psiFile = (factory as PsiFileFactoryImpl).trySetupPsiForFile(
            lvf, JsonLanguage.INSTANCE, false, true
        )
        assert(psiFile != null)
        return psiFile!!
    }


}

object MyJsonPsiUtil {
    //删除节点，包括,号
    fun removeJsonProperty(element: JsonProperty) {
        val next = runReadAction { element.nextSibling }
        if (next != null && next is LeafPsiElement && next.text == ",") {
            next.delete()
        }
        element.delete()
    }
}