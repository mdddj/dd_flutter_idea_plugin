package shop.itbug.fluttercheckversionx.reference

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.psi.impl.DartStringLiteralExpressionImpl
import shop.itbug.fluttercheckversionx.inlay.dartfile.DartStringIconShowInlay
import shop.itbug.fluttercheckversionx.util.DartPsiElementHelper
import shop.itbug.fluttercheckversionx.util.string


class AssetReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(DartStringLiteralExpressionImpl::class.java),
            AssetReferenceProvider()
        )
    }
}

internal class AssetReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        val dartStringLiteral = element as? DartStringLiteralExpressionImpl ?: return PsiReference.EMPTY_ARRAY
        val fileResult = DartPsiElementHelper.checkHasFile(dartStringLiteral)
        if (fileResult != null) {
            return arrayOf<PsiReference>(AssetPsiReference(dartStringLiteral, fileResult))
        }
        return PsiReference.EMPTY_ARRAY
    }
}

internal class AssetPsiReference(
    element: DartStringLiteralExpressionImpl,
    private val fileResult: DartStringIconShowInlay.FileResult
) :
    PsiReferenceBase<PsiElement?>(element, TextRange(1, element.string!!.length + 1)) {
    override fun resolve(): PsiElement? {
        val project = runReadAction { element.project }
        val relativePath = fileResult.basePath
        val projectRoot = runReadAction { project }.guessProjectDir() ?: return null
        val targetFile = projectRoot.findFileByRelativePath(relativePath) ?: return null
        return runReadAction { PsiManager.getInstance(project).findFile(targetFile) }
    }
}
