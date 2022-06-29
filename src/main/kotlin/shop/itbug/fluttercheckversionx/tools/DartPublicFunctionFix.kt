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
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.DartElementType
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartType
import com.jetbrains.lang.dart.psi.impl.*
import com.jetbrains.lang.dart.util.DartPsiImplUtil
import com.jetbrains.lang.dart.util.DartResolveUtil
import io.flutter.dart.DartPsiUtil


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

    override fun visitElement(element: PsiElement) {
        //如果节点是一个类
        if(element is DartClassDefinitionImpl ) {

            //判断是不是继承自StatefulWidget类
           val superClassList = element.childrenOfType<DartSuperclassImpl>()
            if(superClassList.isNotEmpty()){
                val superClassName = superClassList.first().childrenOfType<DartTypeImpl>().first().text
                if(checkSuperClassNames.contains(superClassName)){
                    ///包含需要待检测的类
                   val methods = element.childrenOfType<DartClassBodyImpl>().first().childrenOfType<DartClassMembersImpl>()
                       .first().childrenOfType<DartMethodDeclarationImpl>()
                   val names = methods.filter { it.childrenOfType<DartComponentNameImpl>().first().text == "createState" }
                    if(names.isNotEmpty()){
                        val dmdElement = names.first() //DartMethodDeclarationImpl节点
                        val name =  dmdElement.childrenOfType<DartComponentNameImpl>().first().text
                        if(name.equals("createState")){
                           val returnTypes = dmdElement.childrenOfType<DartReturnTypeImpl>()
                            if(returnTypes.isNotEmpty()){
                                val returnTypeEle = returnTypes.first()
                                if(returnTypeEle.text.indexOf("_") == 0){
                                    holder.registerProblem(returnTypes.first(),"dart建议将返回类型修改为公有变量. QQ自学群:667186542欢迎加入",
                                    ProblemHighlightType.WARNING,PublicApiRenameFix(returnTypeEle,returnTypeEle.text))
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
class PublicApiRenameFix(val element: PsiElement,var className: String): LocalQuickFixOnPsiElement(element) {
    private val renameText = className.removePrefix("_")

    override fun getFamilyName(): String {
        return "梁典典: 将它重命名为$renameText"
    }
    override fun getText(): String {
        return familyName
    }

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {

       val factory = PsiElementFactory.getInstance(project)
       val newNamePsi = factory.createDummyHolder(renameText, IElementType(
            "text",DartLanguage.INSTANCE
        ),null)


        startElement.replace(newNamePsi)
    }

}