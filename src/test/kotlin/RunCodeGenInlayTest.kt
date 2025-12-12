
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.flutterx.manager.myManagerFun

class RunCodeGenInlayTest : BasePlatformTestCase() {


    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    //是否需要显示 code gen inlay
    fun testShouldShowRunCodeGenInlay() {
        val dartFile = myFixture.configureByFile("code_gen_inlay_test.dart") as DartFile
        val clazzList = PsiTreeUtil.findChildrenOfType(dartFile, DartClassDefinitionImpl::class.java)
        assert(clazzList.size == 1)
        val testClass = clazzList.first()
        val clazzManager = testClass.myManagerFun()
        val hasCopyWith = clazzManager.hasMetadataByName("CopyWith")
        assert(hasCopyWith)
    }

}