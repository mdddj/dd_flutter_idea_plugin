package shop.itbug.fluttercheckversionx.autoCompletion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.thisLogger
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
                .withText(PlatformPatterns.string().startsWith("par"))
                .withText(PlatformPatterns.string().startsWith("p")),
            Provider()
        )
    }

    private class Provider : CompletionProvider<CompletionParameters>() {
        private val logger = thisLogger()
        override fun addCompletions(
            parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
        ) {
            val project = parameters.editor.project ?: return
            val partService = UserDartLibService.getInstance(project)
            val file = parameters.editor.virtualFile ?: return

            val libNames = partService.getLibraryNames()


            libNames.forEach { name ->
                val psi = createElement(name, project) { this }
                result.addElement(psi)
            }
            logger.info("lib names: $libNames")
            val rf = partService.calcRelativelyPath(file)
            println("列表:${rf}")
            rf.forEach { model ->
                println("path =>${model.path}  <>${model.libName}")
                result.addElement(createElement("'${model.path}'", project) {
                    if (model.libName.isNotBlank()) {
                        this.withTypeText(model.libName)
                    } else {
                        this.withTypeText(model.file.path, false)
                    }
                    this
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

