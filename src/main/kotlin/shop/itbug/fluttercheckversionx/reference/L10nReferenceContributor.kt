package shop.itbug.fluttercheckversionx.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import shop.itbug.fluttercheckversionx.services.ArbFile
import shop.itbug.fluttercheckversionx.services.L10nKeyItem

/**
 * l10n 关键字提示
 */
class L10nReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(p0: PsiReferenceRegistrar) {
        p0.registerReferenceProvider(
            PlatformPatterns.psiElement(), L10nReferenceProvider(), PsiReferenceRegistrar.HIGHER_PRIORITY
        )
    }
}

internal class L10nReferenceProvider() : PsiReferenceProvider() {
    override fun getReferencesByElement(
        p0: PsiElement,
        p1: ProcessingContext
    ): Array<out PsiReference?> {
        println("${p0.text}")
        return PsiReference.EMPTY_ARRAY
    }
}


internal class L10nKeyItemReference(arbFile: ArbFile, item: L10nKeyItem) : PsiReferenceBase<PsiElement>(
    arbFile.originPsiFile,
    item.range
) {
    init {
        println("进来了。")
    }

    override fun resolve(): PsiElement? {
        return null
    }

}

//        val project = p0.project
//        val services = FlutterL10nService.getInstance(project)
//        val items = services.arbFiles
//        if (items.isEmpty()) {
//            return PsiReference.EMPTY_ARRAY
//        }
//        val arr = arrayListOf<PsiReference>()
//        for (item in items) {
//            val find = item.keyItems.find { it.key == p0.text }
//            if (find == null) {
//                continue
//            }
//            arr.add(L10nKeyItemReference(item, find))
//        }
//        return arr.toTypedArray()