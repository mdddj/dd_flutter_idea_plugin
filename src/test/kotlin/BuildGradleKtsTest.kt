
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import shop.itbug.fluttercheckversionx.inlay.gradle.aliyunGradleMirrorImages
import shop.itbug.fluttercheckversionx.util.MyKotlinPsiElementFactory


class BuildGradleKtsTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData/project"
    }

    fun testAddAliyunMirrorImage() {
        val ktFile = myFixture.configureByText(
            "build.gradle.kts",
            """
                allprojects {
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
            """.trimIndent(),
        ) as KtFile


        val ktCalls = PsiTreeUtil.findChildrenOfType(ktFile, KtCallExpression::class.java)
        val findRepo = ktCalls.find { it.calleeExpression?.text == "repositories" }
        assertNotNull(findRepo != null)
        findRepo ?: return


        val factory = MyKotlinPsiElementFactory(project)
        val ktFunLit = findRepo.lambdaArguments.firstOrNull()?.getLambdaExpression()?.functionLiteral
        assert(ktFunLit != null)
        if (ktFunLit != null) {
            val block = ktFunLit.bodyExpression ?: return
            val ele = aliyunGradleMirrorImages.map(factory::createCallExpression)
            val last = block.statements.lastOrNull() ?: return

            WriteCommandAction.runWriteCommandAction(project) {
//                ele.forEach(block::add)
                ele.forEach {
                    block.addAfter(it, last)
                }
            }
            println(ktFile.text)
        }

    }

}