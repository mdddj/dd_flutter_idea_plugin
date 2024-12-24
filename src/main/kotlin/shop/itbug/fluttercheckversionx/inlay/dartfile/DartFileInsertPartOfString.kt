package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.InheritorsCodeVisionProvider
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartPartOfStatementImpl
import com.jetbrains.lang.dart.psi.impl.DartPartStatementImpl
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.awt.event.MouseEvent
import java.nio.file.Paths

/**
 * part of 自动插入part of 语句
 */
class DartFileInsertPartOfString : InheritorsCodeVisionProvider() {

    override fun acceptsFile(file: PsiFile): Boolean {
        return file is DartFile
    }

    override fun acceptsElement(element: PsiElement): Boolean {
        return element is DartPartOfStatementImpl && element.libraryFiles.isNotEmpty() && element.parts().isNotEmpty()
                && validIsShow(element, element.parts())
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? {
        return "add part statement"
    }

    override fun handleClick(
        editor: Editor, element: PsiElement, event: MouseEvent?
    ) {
        val last = (element as DartPartOfStatementImpl).parts().last()
        val createPsi = generatePartStatement(last, element) ?: return
        writeToTarget(element.project, last, createPsi)
    }

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = emptyList()
    override val id: String
        get() = "DartFileInsertPartOfString"
}

private fun DartPartOfStatementImpl.findTargetFile(): PsiFile? {
    return libraryFiles.firstOrNull()?.let { PsiManager.getInstance(project).findFile(it) }
}

private fun PsiFile.parts(): List<DartPartStatementImpl> {
    return PsiTreeUtil.findChildrenOfAnyType(
        this, DartPartStatementImpl::class.java
    ).filterNotNull()
}

private fun DartPartOfStatementImpl.parts(): List<DartPartStatementImpl> {
    val file = findTargetFile() ?: return emptyList()
    return file.parts()
}

private fun validIsShow(element: PsiElement, parts: List<DartPartStatementImpl>): Boolean {
    val file = (element as DartPartOfStatementImpl).findTargetFile() ?: return false
    val text = generatePartStatementText(parts.last(), element) ?: return false
    return !file.findByText(text)
}

private fun getRelativeOrFileName(path1: String, path2: String): String {
    val basePath = Paths.get(path1).parent
    val targetPath = Paths.get(path2)

    if (basePath == targetPath.parent) {
        return targetPath.fileName.toString()
    } else {
        val relativePath = basePath.relativize(targetPath).toString()
        return relativePath.ifEmpty { "./${targetPath.fileName}" }
    }
}

private fun createPartOf(s: String, project: Project): DartPartStatementImpl? {
    return MyDartPsiElementUtil.createDartPart(s, project)
}


private fun generatePartStatementText(part: DartPartStatementImpl, element: PsiElement): String? {
    val p1 = part.containingFile.virtualFile.path
    val p2 = element.containingFile.virtualFile.path
    val rl = getRelativeOrFileName(p1, p2)
    return "part '$rl';"
}

private fun generatePartStatement(part: DartPartStatementImpl, element: PsiElement): DartPartStatementImpl? {
    val text = generatePartStatementText(part, element) ?: return null
    val createPartOf = createPartOf(text, project = element.project)
    return createPartOf
}

// true: 已存在
private fun PsiFile.findByText(text: String): Boolean {
    return PsiTreeUtil.findChildrenOfAnyType(this, DartPartStatementImpl::class.java)
        .find { it.text == text } != null
}

//写入文件
private fun writeToTarget(project: Project, lastPart: DartPartStatementImpl, newPart: DartPartStatementImpl) {
    WriteCommandAction.runWriteCommandAction(project) {
        lastPart.addAfter(newPart, null)
    }
}