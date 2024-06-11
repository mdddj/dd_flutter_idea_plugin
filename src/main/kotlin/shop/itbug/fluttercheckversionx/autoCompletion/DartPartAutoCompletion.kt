package shop.itbug.fluttercheckversionx.autoCompletion

import UserDartLibService
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * part lib自动完成提供者
 */
class DartPartAutoCompletion : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    p0: CompletionParameters,
                    p1: ProcessingContext,
                    p2: CompletionResultSet
                ) {
                    val project = p0.editor.project!!
                    val libNames = UserDartLibService.getInstance(project).getLibraryNames()
                    libNames.forEach {
                        val ele =
                            LookupElementBuilder.create("part of $it").withIcon(
                                MyIcons.flutter
                            ).withPsiElement(
                                MyDartPsiElementUtil.createDartPart(
                                    "part of $it",
                                    project
                                )
                            )
                        p2.addElement(ele)
                    }

                }

            }
        )
    }
}

