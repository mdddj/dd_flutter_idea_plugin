package shop.itbug.fluttercheckversionx.fold

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.fluttercheckversionx.util.MyPsiElementUtil

class PubYamlPathAndGithubFold: FoldingBuilderEx(),DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val array = mutableListOf<FoldingDescriptor>()
        if(root is YAMLFile){
            val allFlutters = MyPsiElementUtil.getAllFlutters(project = root.project)

            if(allFlutters.isNotEmpty()){
                allFlutters.forEach { (_, u) ->
                    u.forEach {
                        val ele = it.element
//                        array.add(FoldingDescriptor(ele, TextRange(ele.startOffset,ele.endOffset)))

                    }

                }

            }
        }


        println("多少个折叠:"+array.size)
        return array.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        return "本地插件和github插件已被折叠"
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true
    }
}