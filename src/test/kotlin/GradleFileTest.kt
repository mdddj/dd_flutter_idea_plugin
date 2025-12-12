
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import shop.itbug.flutterx.inlay.gradle.aliyunGradleMirrorImages
import shop.itbug.flutterx.util.GrPsiElementHelper
import shop.itbug.flutterx.util.MyGrPsiElementFactory

class GradleFileTest : BasePlatformTestCase() {
    override fun getTestDataPath():  String {
        return "src/test/testData/project"
    }


    fun findBuildGradleKtsFile(): VirtualFile {
        return myFixture.configureByFile("build.gradle.kts").virtualFile
    }

    fun findBuildGradleFile(): VirtualFile {
        return myFixture.configureByFile("build.gradle").virtualFile
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

    fun testCreateGVFile() {
        val file = GroovyPsiElementFactory.getInstance(project)
            .createGroovyFile("maven { url 'https://maven.aliyun.com/repository/google' }", true, null)
        println("file=$file")
        val psi = PsiTreeUtil.findChildOfType(file, GrMethodCallExpression::class.java)!!
        assertNotNull(psi)
    }


    fun testAddAliYunMirror() {
        val grPsiFile = GroovyPsiElementFactory.getInstance(project)
            .createGroovyFile("""
                allprojects {
                    repositories {
                        maven { url 'https://maven.zohodl.com' }
                        mavenLocal()
                        mavenCentral()
                    }
                }
            """.trimIndent(),true,null)
        val factory = MyGrPsiElementFactory(project)
        val methodCallExpression = PsiTreeUtil.findChildrenOfAnyType(grPsiFile, GrMethodCallExpression::class.java).toList()
        val repositoriesMethodCall = methodCallExpression.find { it.invokedExpression.text == "repositories" }
        assertNotNull(repositoriesMethodCall)
        repositoriesMethodCall ?: return
        GrPsiElementHelper.methodCallAddMethodCall(
            project,
            repositoriesMethodCall,
            aliyunGradleMirrorImages.reversed().map(factory::createMethodCall)
        )
        Thread.sleep(1000)
        println(grPsiFile.text)
    }
}