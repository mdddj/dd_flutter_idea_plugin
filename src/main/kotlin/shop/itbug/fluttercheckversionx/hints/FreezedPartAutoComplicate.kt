package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import shop.itbug.fluttercheckversionx.icons.MyIcons

class FreezedPartAutoComplicate : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE),
            FreezedPartAutoComplicateProvider()
        )
    }
}

class FreezedPartAutoComplicateProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val name = parameters.originalFile.name.removeSuffix(".dart")
        val isPartText = result.prefixMatcher.prefix == "pf"
        if (isPartText) {
            val create = LookupElementBuilder.create("part '$name.freezed.dart'; ").withIcon(MyIcons.diandianLogoIcon)
            result.addElement(create)
        }

        val isGen = result.prefixMatcher.prefix == "pg"
        if (isGen) {
            val create = LookupElementBuilder.create("part '$name.g.dart'; ").withIcon(MyIcons.diandianLogoIcon)

            result.addElement(create)
        }
    }

}