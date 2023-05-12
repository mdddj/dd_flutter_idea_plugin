package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
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


        ///构造函数添加
        val offset = parameters.editor.caretModel.offset
        val findElementAt = parameters.originalFile.findElementAt(offset)
        if (findElementAt?.parent?.parent is DartClassDefinitionImpl) {
            val classPsi = findElementAt.parent.parent as DartClassDefinitionImpl
            println(result.prefixMatcher.prefix == "cc")
            if (result.prefixMatcher.prefix == "cc") {

                result.addElement(
                    LookupElementBuilder.create("${classPsi.componentName}._();").withIcon(MyIcons.diandianLogoIcon)
                )
            }
        }

    }

}