import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.DartFile


//dart 分析测试
class DartAnalysTest: BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    //测试元素是不是一个枚举
    fun testCheckIsEnum(){
        val dotFile = myFixture.configureByFile("dot.dart") as DartFile
        println(dotFile.text)
        val r = DartAnalysisServerService.getInstance(project).analysis_getHover(dotFile.virtualFile,20)
        println(r)

    }
}