import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny

class GradleFileTest : BasePlatformTestCase() {
    override fun getTestDataPath(): @NonNls String {
        return "src/test/testData/project"
    }


    fun findBuildGradleKtsFile(): VirtualFile {
        return myFixture.configureByFile("build.gradle.kts").virtualFile
    }




    fun testCheck() {
        val ktsFile = findBuildGradleKtsFile()
        val psi = PsiManager.getInstance(project).findFile(ktsFile) as KtFile
        val script = psi.script
        assertNotNull(script)
        val scriptBlocks = (script as KtScript).blockExpression.children.filterIsInstance<KtScriptInitializer>()
        assertEquals(5, scriptBlocks.size)

        val find = scriptBlocks.find {
            it.body?.getCalleeExpressionIfAny()?.text == "allprojects"
        }
        println(find)

    }
}