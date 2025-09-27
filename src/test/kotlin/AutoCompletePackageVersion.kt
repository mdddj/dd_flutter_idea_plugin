
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

//自动补全版本号
class AutoCompletePackageVersion : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    fun test() {
        myFixture.configureByFile("pubspec.yaml")
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        println(lookupElementStrings)
    }
}