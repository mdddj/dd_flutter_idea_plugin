import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import shop.itbug.flutterx.inlay.gradle.aliyunKtsMirrorImages
import shop.itbug.flutterx.util.MyKotlinPsiElementFactory
import shop.itbug.flutterx.util.reformatText


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
            val ele = aliyunKtsMirrorImages.map(factory::createCallExpression)
            var last: PsiElement = block.statements.lastOrNull() ?: return
            val whiteSpaceFromText = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n")
            WriteCommandAction.runWriteCommandAction(project) {
                last = block.addAfter(whiteSpaceFromText, last)
                ele.forEach {
                    last = block.addAfter(it, last)
                    last = block.addAfter(whiteSpaceFromText, last)
                }
            }
            // 格式化
            ktFile.reformatText()
            println(ktFile.text)
        }

    }

}