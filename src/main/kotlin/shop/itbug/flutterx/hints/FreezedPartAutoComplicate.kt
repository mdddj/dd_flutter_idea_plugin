package shop.itbug.flutterx.hints

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.impl.DartClassBodyImpl
import com.jetbrains.lang.dart.psi.impl.DartClassDefinitionImpl
import shop.itbug.flutterx.icons.MyIcons
import shop.itbug.flutterx.manager.myManagerFun
import shop.itbug.flutterx.util.DartPsiElementUtil

class FreezedPartAutoComplicate : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(DartLanguage.INSTANCE)
                .withSuperParent(6, DartFile::class.java),
            FreezedPartAutoComplicateProvider()
        )

        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(DartLanguage.INSTANCE).withSuperParent(7, DartClassBodyImpl::class.java)
                .withSuperParent(8, DartClassDefinitionImpl::class.java),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val cls = parameters.getDartClassDefine()
                    val manager = cls?.myManagerFun()
                    if (manager?.hasFreezeMetadata() == true) {
                        val className = cls.getDartClassName()
                        className?.let {
                            result.withPrefixMatcher("ff").addElement(
                                LookupElementBuilder.create("factory $className.fromJson(Map<String, dynamic> json) => _$${className}FromJson(json);")
                                    .withIcon(MyIcons.flutter)
                            )

                            result.withPrefixMatcher("fc")
                                .addElement(LookupElementBuilder.create("${className}._();").withIcon(MyIcons.flutter))
                        }
                    }


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


private fun CompletionParameters.getDartClassDefine(): DartClassDefinitionImpl? {
    return DartPsiElementUtil.findParentElementOfType(
        originalFile.findElementAt(offset)!!,
        DartClassDefinitionImpl::class.java
    ) as? DartClassDefinitionImpl
}

private fun DartClassDefinitionImpl?.getDartClassName(): String? {
    return this?.componentName?.name
}

