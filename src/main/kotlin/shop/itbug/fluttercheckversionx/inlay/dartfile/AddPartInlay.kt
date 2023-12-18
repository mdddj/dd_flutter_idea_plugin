package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.lang.dart.psi.impl.DartPartOfStatementImpl
import com.jetbrains.lang.dart.psi.impl.DartPartStatementImpl
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil
import java.nio.file.Paths
import javax.swing.JComponent


/// part 操作
class AddPartInlay : InlayHintsProvider<AddPartInlay.AddPartInlaySetting> {


    data class AddPartInlaySetting(var show: Boolean = true)

    override val key: SettingsKey<AddPartInlaySetting>
        get() = SettingsKey("part action inlay")
    override val name: String
        get() = "part action"
    override val previewText: String
        get() = """
            part of "test.dart"
        """.trimIndent()

    override fun createSettings(): AddPartInlaySetting = AddPartInlaySetting()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: AddPartInlaySetting,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val myFactory = HintsInlayPresentationFactory(factory)
                if (element is DartPartOfStatementImpl) {


                    val files = element.libraryFiles
                    if (files.isNotEmpty()) {
                        val first = files.first()
                        val libFilePsi: PsiFile? = PsiManager.getInstance(element.project).findFile(first)
                        if (libFilePsi != null) {
                            val parts =
                                PsiTreeUtil.findChildrenOfAnyType(libFilePsi, DartPartStatementImpl::class.java)
                            if (parts.isNotEmpty()) {
                                val lastPart = parts.last() ///最后一个 part
                                val p1 = lastPart.containingFile.virtualFile.path
                                val p2 = element.containingFile.virtualFile.path
                                val rl = getRelativeOrFileName(p1, p2)
                                val createPartOf = createPartOf("$rl", project = element.project)

                                ///如果存在了就不要显示了
                                val findPart =
                                    PsiTreeUtil.findChildrenOfAnyType(libFilePsi, DartPartStatementImpl::class.java)
                                        .find { it.text == createPartOf?.text }
                                if (findPart == null) {
                                    val inlayPresentation = myFactory.simpleText(
                                        "FlutterX: inset part \"${rl}\" to library ${element.libraryName}",
                                        null
                                    ) { p1, p2 ->
                                        createPartOf?.let {
                                            WriteCommandAction.runWriteCommandAction(element.project) {
                                                lastPart.addAfter(createPartOf, null)
                                            }
                                        }
                                    }
                                    sink.addInlineElement(element.endOffset, true, inlayPresentation, true)
                                }
                            }
                        }

                    }
                }
                return true
            }

            private fun createPartOf(s: String, project: Project): DartPartStatementImpl? {
                return MyDartPsiElementUtil.createDartPart("part '$s';", project)
            }
        }
    }

    override fun createConfigurable(settings: AddPartInlaySetting): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return panel {
                    row("action") {
                        checkBox("enable").bindSelected(settings::show)
                    }
                }
            }

        }
    }
}


/**
 *
 * 获取相对路径
 * ```kotlin
 * fun main() {
 *     val basePath = "/lib/abc"
 *     val targetPath = "/lib/bac/file.txt"
 *
 *     val relativePath = getRelativePath(basePath, targetPath)
 *     println(relativePath)
 * }
 * ```
 */
fun getRelativeOrFileName(path1: String, path2: String): String {
    val basePath = Paths.get(path1).parent
    val targetPath = Paths.get(path2)

    if (basePath == targetPath.parent) {
        return targetPath.fileName.toString()
    } else {
        val relativePath = basePath.relativize(targetPath).toString()
        return if (relativePath.isNotEmpty()) relativePath else "./${targetPath.fileName}"
    }
}