package tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import common.YamlFileParser
import kotlinx.coroutines.runBlocking

/**
 * yaml 版本自动补全
 */
class AutoVersionTool : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return YamlElementVisitor(holder, isOnTheFly)
    }
}

class YamlElementVisitor(
    private val holder: ProblemsHolder,
    private val isOnTheFly: Boolean
) : PsiElementVisitor() {


    override fun visitFile(file: PsiFile) {
        if (!isOnTheFly) return


        runBlocking {
            val yamlFileParser: YamlFileParser = YamlFileParser(file,holder)
            yamlFileParser.startCheckFile()
        }

        super.visitFile(file)

    }



}