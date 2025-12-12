package shop.itbug.flutterx.inlay.yaml

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.codeInsight.hints.codeVision.CodeVisionProviderBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.util.MyActionUtil
import shop.itbug.flutterx.util.MyFileUtil
import java.awt.event.MouseEvent

class DartPackageSearchDialogCodeVision : CodeVisionProviderBase() {
    override fun acceptsFile(file: PsiFile): Boolean {
        return MyFileUtil.isFlutterPubspecFile(file)
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        val isDepEle = element is YAMLKeyValue && element.keyText == "dependencies"
        return isDepEle
    }

    override fun getHint(element: PsiElement, file: PsiFile): String {
        return "Add package"
    }

    override fun handleClick(
        editor: Editor,
        element: PsiElement,
        event: MouseEvent?
    ) {
        editor.project?.let {
            val file = PsiManager.getInstance(it).findFile(element.containingFile.virtualFile) ?: return@let
            MyActionUtil.showPubSearchDialog(it, file as YAMLFile)
        }
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
                    icon = MyIcons.dartPackageIcon,
                )
            )
        }
    }

    override val name: String
        get() = "Flutter Pub Search Dialog"
    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingLast)
    override val id: String
        get() = "DartPackageSearchDialogCodeVision"
}

class DartPackageSearchDialogCodeVisionGroupSetting: CodeVisionGroupSettingProvider{
    override val groupId: String
        get() = "DartPackageSearchDialogCodeVision"

    override val groupName: String
        get() = "Pub Package Search (flutterX)"

    override val description: String
        get() = "Show Pub.dev search dialog "
}