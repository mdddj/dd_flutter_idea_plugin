package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartFile
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.services.UserDartLibService
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
            parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
        ) {
            val project = parameters.editor.project!!
            val partService = UserDartLibService.getInstance(project)
            val file = parameters.editor.virtualFile

            partService.getLibraryNames().forEach { name ->
                val psi = createElement(name, project) { this }
                result.addElement(psi)
            }
            partService.calcRelativelyPath(file).forEach { model ->
                result.addElement(createElement("'${model.path}'", project) {
                    this.withTypeText(model.libName)
                })
            }
        }

        private fun createElement(
            name: String,
            project: Project,
            init: LookupElementBuilder.() -> LookupElementBuilder
        ): LookupElement {
            return LookupElementBuilder.create("part of $name").withIcon(MyIcons.flutter).init().withPsiElement(
                MyDartPsiElementUtil.createDartPart(
                    "part of $name", project
                )
            )
        }
    }


}

