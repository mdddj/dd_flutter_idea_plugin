package shop.itbug.fluttercheckversionx.autoCompletion

import UserDartLibService
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartFile
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * part lib自动完成提供者
 * 举例: part of dev;
 */
class DartPartAutoCompletion : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE).withSuperParent(6, DartFile::class.java)
                .withText(PlatformPatterns.string().startsWith("par")),
            Provider()
        )
    }

    private inner class Provider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val project = parameters.editor.project!!
            val libNames = UserDartLibService.getInstance(project).getLibraryNames()
            libNames.forEach {
                val psi = createElement(it, project)
                result.addElement(psi)
            }
        }

        private fun createElement(name: String, project: Project): LookupElement {
            return LookupElementBuilder.create("part of $name").withIcon(
                MyIcons.flutter
            ).withPsiElement(
                MyDartPsiElementUtil.createDartPart(
                    "part of $name",
                    project
                )
            )
        }
    }


}

