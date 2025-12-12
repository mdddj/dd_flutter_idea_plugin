package shop.itbug.flutterx.inlay.gradle

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiParserFacade
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression
import shop.itbug.flutterx.i18n.PluginBundle
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.MyKotlinPsiElementFactory
import shop.itbug.flutterx.util.reformatText
import java.awt.event.MouseEvent

val aliyunKtsMirrorImages = listOf(
    "maven(\"https://maven.aliyun.com/repository/google\")",
    "maven(\"https://maven.aliyun.com/repository/public\")",
    "maven(\"https://maven.aliyun.com/repository/jcenter\")",
    "maven(\"https://maven.aliyun.com/repository/central\")",
    "maven(\"https://maven.aliyun.com/repository/gradle-plugin\")",
    """
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/groups/public")
            isAllowInsecureProtocol = true
        }
    """.trimIndent(),
    """
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
    """.trimIndent(),
    """
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/repositories/releases/")
            isAllowInsecureProtocol = true
        }
    """.trimIndent()
)
class KtsAliyunMirrorInlay : CodeVisionProviderBase() {
    override fun acceptsFile(file: PsiFile): Boolean {
        return file.fileType == KotlinFileType.INSTANCE && file.virtualFile.extension == "kts"
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        return element is KtCallExpression && element.calleeExpression?.text == "repositories"
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return PluginBundle.get("inlay.change.to.aliyun.mirror.image")
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

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        val project = element.project
        val findRepo = element as KtCallExpression
        val factory = MyKotlinPsiElementFactory(project)
        val ktFunLit = findRepo.lambdaArguments.firstOrNull()?.getLambdaExpression()?.functionLiteral
        assert(ktFunLit != null)
        if (ktFunLit != null) {
            val block = ktFunLit.bodyExpression ?: return
            var last: PsiElement = block.statements.lastOrNull() ?: return
            val ele = aliyunKtsMirrorImages.map(factory::createCallExpression)
            val whiteSpaceFromText = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n")
            WriteCommandAction.runWriteCommandAction(project) {
                last = block.addAfter(whiteSpaceFromText, last)
                ele.forEach {
                    last = block.addAfter(it, last)
                    last = block.addAfter(whiteSpaceFromText, last)
                }
            }
            ktFunLit.reformatText()
        }
    }

    override val name: String
        get() = "Change Aliyun Mirror"
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingLast)
    override val id: String
        get() = "flutter-aliyun-mirror-image-inlay-kts"
}

class KtsAliyunMirrorInlayGroupSetting: CodeVisionGroupSettingProvider {
    override val groupId: String
        get() = "flutter-aliyun-mirror-image-inlay-kts"

    override val groupName: String
        get() = "Change Aliyun Mirror (flutterx)"

    override val description: String
        get() = "Quickly add Aliyun mirror (kts)"
}