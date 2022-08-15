package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiElementFilter
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.impl.*


val checkSuperClassNames = listOf<String>("StatefulWidget")

/**
 * 自动修复功能: Avoid using private types in public APIs.
 * 解释: [https://dart-lang.github.io/linter/lints/library_private_types_in_public_api.html]
 */
class DartPublicFunctionFix : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return DartPublicFunctionApiFixVisitor(holder = holder)
    }


}

class DartPublicFunctionApiFixVisitor(val holder: ProblemsHolder) : PsiElementVisitor() {

    fun getMethodCallRefre(dmdElement: DartMethodDeclarationImpl): PsiElement? {
        ///获取返回值
        val dartFuntionBodyList = dmdElement.childrenOfType<DartFunctionBodyImpl>()
        if (dartFuntionBodyList.isNotEmpty()) {
            val dartCallExpressionList = dartFuntionBodyList.first().childrenOfType<DartCallExpressionImpl>()
            if (dartCallExpressionList.isNotEmpty()) {
                val dartReferenceExpessionList =
                    dartCallExpressionList.first().childrenOfType<DartReferenceExpressionImpl>()
                if (dartReferenceExpessionList.isNotEmpty()) {
                    return dartReferenceExpessionList.first()
                }
            }
        }
        return null
    }

    override fun visitElement(element: PsiElement) {
        //如果节点是一个类
        if (element is DartClassDefinitionImpl) {

            //判断是不是继承自StatefulWidget类
            val superClassList = element.childrenOfType<DartSuperclassImpl>()
            if (superClassList.isNotEmpty()) {
                val c2 = superClassList.first().childrenOfType<DartTypeImpl>()
                if (c2.isNotEmpty()) {
                    val superClassName = c2.first().text
                    if (checkSuperClassNames.contains(superClassName)) {
                        ///包含需要待检测的类
                        val methods =
                            element.childrenOfType<DartClassBodyImpl>().first().childrenOfType<DartClassMembersImpl>()
                                .first().childrenOfType<DartMethodDeclarationImpl>()
                        val names =
                            methods.filter { it.childrenOfType<DartComponentNameImpl>().first().text == "createState" }
                        if (names.isNotEmpty()) {
                            val dmdElement = names.first() //DartMethodDeclarationImpl节点
                            val name = dmdElement.childrenOfType<DartComponentNameImpl>().first().text
                            if (name.equals("createState")) {
                                val returnTypes = dmdElement.childrenOfType<DartReturnTypeImpl>()
                                if (returnTypes.isNotEmpty()) {
                                    val returnTypeEle = returnTypes.first()
                                    if (returnTypeEle.text.indexOf("_") == 0) {
                                        holder.registerProblem(
                                            returnTypes.first(),
                                            "dart建议将返回类型修改为公有变量. Flutter自学群:667186542",
                                            ProblemHighlightType.WARNING,
                                            PublicApiRenameFix(returnTypeEle, returnTypeEle.text, dmdElement)
                                        )
                                    }

                                }
                            }


                        }
                    }
                }

            }
        }
        super.visitElement(element)
    }
}

///修复类
class PublicApiRenameFix(val element: PsiElement, var className: String, var functionElement: PsiElement) :
    LocalQuickFixOnPsiElement(element) {
    private val renameText = className.removePrefix("_")

    override fun getFamilyName(): String {
        return "重命名为$renameText"
    }

    override fun getText(): String {
        return familyName
    }

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {

        val factory = PsiElementFactory.getInstance(project)

        val newNamePsi = factory.createDummyHolder(renameText, DartTokenTypes.RETURN_TYPE, null)
        startElement.replace(newNamePsi)


        val find = PsiTreeUtil.collectElements(functionElement, object : PsiElementFilter {
            override fun isAccepted(element: PsiElement): Boolean {
                if (element is DartReferenceExpressionImpl && element.text.equals(className)) {
                    return true
                }
                return false
            }
        })

        if (find.isNotEmpty()) {
            for (i in find) {
                val newEl = factory.createDummyHolder(renameText, DartTokenTypes.REFERENCE_EXPRESSION, null)
                i.replace(newEl)
            }
        }


        val classfind = PsiTreeUtil.collectElementsOfType(file.originalElement, DartComponentNameImpl::class.java)
        println(classfind.size)
        for (c in classfind) {
            if (c.text.equals(className)) {
                val newEl = factory.createDummyHolder(renameText, DartTokenTypes.COMPONENT_NAME, null)
                c.replace(newEl)
            }
        }

    }

}


inline fun <reified T : PsiElement> PsiElement.childrenOfType(): List<PsiElement> {
    return this.children.filterIsInstance<T>()
}