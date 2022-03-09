package tools

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import common.YamlFileParser
import kotlinx.coroutines.*

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

    @OptIn(DelicateCoroutinesApi::class)
    override fun visitFile(file: PsiFile) {
        if (!isOnTheFly) return


        /// 另外开一个线程执行检测
        runBlocking(Dispatchers.Main){

            launch {
                val yamlFileParser = YamlFileParser(file,holder)
                yamlFileParser.startCheckFile()
            }
        }




        super.visitFile(file)

    }



}