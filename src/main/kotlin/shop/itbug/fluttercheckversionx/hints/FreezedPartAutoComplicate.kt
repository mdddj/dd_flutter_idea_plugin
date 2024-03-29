package shop.itbug.fluttercheckversionx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.impl.DartClassBodyImpl
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import groovy.util.logging.Slf4j
import shop.itbug.fluttercheckversionx.icons.MyIcons
import shop.itbug.fluttercheckversionx.util.DartPsiElementUtil

@Slf4j
class FreezedPartAutoComplicate : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE),
            FreezedPartAutoComplicateProvider()
        )

        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withSuperParent(7,DartClassBodyImpl::class.java)
                .withLanguage(DartLanguage.INSTANCE),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                    val className = parameters.getDartClassName()
                    result.withPrefixMatcher("ff").addElement(LookupElementBuilder.create("factory $className.fromJson(Map<String, dynamic> json) => _\$${className}FromJson(json);").withIcon(MyIcons.diandianLogoIcon))

                    result.withPrefixMatcher("fc").addElement(LookupElementBuilder.create("${className}._();").withIcon(MyIcons.diandianLogoIcon))
                }
            }

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


///获取类名
fun CompletionParameters.getDartClassName() : String {
    val findParentElementOfType =
        DartPsiElementUtil.findParentElementOfType(originalFile.findElementAt(offset)!!, DartClassDefinitionImpl::class.java) as? DartClassDefinitionImpl
    return findParentElementOfType?.componentName?.name ?: "null"
}