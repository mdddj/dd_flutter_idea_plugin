package tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import common.YamlFileParser
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * yaml 版本自动补全
 */
class AutoVersionTool : LocalInspectionTool() {

    /// 访问了文件
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        println("访问了文件")
        return YamlElementVisitor(holder, isOnTheFly)
    }
}

class YamlElementVisitor(
    private val holder: ProblemsHolder,
    private val isOnTheFly: Boolean
) : PsiElementVisitor() {


    override fun visitFile(file: PsiFile) {
        if (!isOnTheFly) return


//        print("即将执行携程程序")
//        val yamlFileParser = YamlFileParser(file,holder)
//        yamlFileParser.cancelAll()
//        yamlFileParser.launch {
//            yamlFileParser.startCheckFile()
//        }
        super.visitFile(file)

    }



}