package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

class IpHostAutoCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(LeafPsiElement::class.java)
                .withParent(DartStringLiteralExpression::class.java)
                .withLanguage(DartLanguage.INSTANCE),
            IPCompletionProvider()
        )
    }

}