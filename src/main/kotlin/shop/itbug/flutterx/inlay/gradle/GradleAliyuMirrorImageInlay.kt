package shop.itbug.flutterx.inlay.gradle

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.GrPsiElementHelper
import shop.itbug.flutterx.util.MyGrPsiElementFactory
import java.awt.event.MouseEvent

 val aliyunGradleMirrorImages = listOf(
    "maven { url 'https://maven.aliyun.com/repository/google' }",
    "maven { url 'https://maven.aliyun.com/repository/public' }",
    "maven { url 'https://maven.aliyun.com/repository/jcenter' }",
    "maven { url 'https://maven.aliyun.com/repository/central' }",
    "maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }",
    """
        maven {
            url 'http://maven.aliyun.com/nexus/content/groups/public'
            allowInsecureProtocol = true
        }
    """.trimIndent(),
    """
        maven {
            url 'http://maven.aliyun.com/nexus/content/groups/public/'
            allowInsecureProtocol = true
        }
    """.trimIndent(),
    """
        maven {
            url 'http://maven.aliyun.com/nexus/content/repositories/releases/'
            allowInsecureProtocol = true
        }
    """.trimIndent()
)

class GradleAliyuMirrorImageInlay : CodeVisionProviderBase() {
    private val logger = thisLogger()
    override fun acceptsFile(file: PsiFile): Boolean {
        return file is GroovyFile
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        return element is GrMethodCallExpression
                && element.hasClosureArguments()
                && element.closureArguments.isNotEmpty()
                && element.invokedExpression.text == "repositories"
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return PluginBundle.get("inlay.change.to.aliyun.mirror.image")
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        val factory = MyGrPsiElementFactory(element.project)
        val methodCallExpression = element as GrMethodCallExpression
        GrPsiElementHelper.methodCallAddMethodCall(
            element.project,
            methodCallExpression,
            aliyunGradleMirrorImages.reversed().map(factory::createMethodCall)
        )
    }


    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        val result = super.computeForEditor(editor, file)
        return result.map {
            val second = it.second as ClickableTextCodeVisionEntry
            Pair(
                first = it.first,
                second = ClickableTextCodeVisionEntry(
                    text = second.text,
                    providerId = second.providerId,
                    onClick = second.onClick,
                    icon = MyIcons.flutter,
                )
            )
        }
    }

    override val name: String
        get() = "Change AliYun Mirror Image"
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingLast)
    override val id: String
        get() = "flutter-aliyun-mirror-image-inlay"
}

class GradleAliyuMirrorImageInlayGroupSetting: CodeVisionGroupSettingProvider {
    override val groupId: String
        get() = "flutter-aliyun-mirror-image-inlay"

    override val groupName: String
        get() = "Change AliYun Mirror Image (flutterX)"

    override val description: String
        get() = "Quickly add Aliyun mirror (Groovy)"

}