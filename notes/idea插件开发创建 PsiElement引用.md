# 需求

按住ctrl+点击资产字符串打开这个文件

# 扩展点

```xml

<psi.referenceContributor
        implementation="shop.itbug.fluttercheckversionx.reference.AssetReferenceContributor"
        language="Dart"
/>
```

# 实现

```kotlin
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

```