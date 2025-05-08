package shop.itbug.fluttercheckversionx.reference

import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FilePathReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper
import shop.itbug.fluttercheckversionx.util.string


class AssetReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(DartStringLiteralExpressionImpl::class.java).with(
                object : PatternCondition<DartStringLiteralExpressionImpl>("Dart Svg Asset") {
                    override fun accepts(
                        element: DartStringLiteralExpressionImpl,
                        context: ProcessingContext
                    ): Boolean {
                        val str = element.string ?: return false
                        return str.endsWith(".svg") && DartPsiElementHelper.checkHasFile(element) != null
                    }
                }
            ),
            AssetReferenceProvider()
        )
    }
}

internal class AssetReferenceProvider : FilePathReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        val dartStringLiteral = element as? DartStringLiteralExpressionImpl ?: return PsiReference.EMPTY_ARRAY
        val fileResult = DartPsiElementHelper.checkHasFile(dartStringLiteral)
        if (fileResult != null) {
            val project = dartStringLiteral.project
            val targetFile = project.guessProjectDir()?.findFileByRelativePath(fileResult.basePath)
                ?: return PsiReference.EMPTY_ARRAY
            val psiFile = PsiManager.getInstance(project).findFile(targetFile)
                ?: return PsiReference.EMPTY_ARRAY
            return arrayOf<PsiReference>(AssetPsiReference(dartStringLiteral, psiFile))
        }
        return PsiReference.EMPTY_ARRAY
    }
}

internal class AssetPsiReference(
    element: DartStringLiteralExpressionImpl,
    val psiFile: PsiFile
) :
    PsiReferenceBase<PsiElement?>(element, TextRange(1, element.string!!.length + 1)) {
    override fun resolve(): PsiElement? {
        return psiFile
    }
}
