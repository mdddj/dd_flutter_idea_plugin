import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartFile
import shop.itbug.flutterx.util.DartLspHoverUtil


//dart 分析测试
class DartAnalysTest: BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    //测试元素是不是一个枚举
    fun testCheckIsEnum(){
        val dotFile = myFixture.configureByFile("dot.dart") as DartFile
        println(dotFile.text)
        val element = dotFile.findElementAt(20) ?: return
        val r = DartLspHoverUtil.getHover(element)
        println(r)

    }
}