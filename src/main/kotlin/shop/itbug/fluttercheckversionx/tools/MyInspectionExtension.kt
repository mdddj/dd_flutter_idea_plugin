package shop.itbug.fluttercheckversionx.tools

import com.intellij.codeInspection.HTMLComposer
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension
import com.intellij.codeInspection.lang.HTMLComposerExtension
import com.intellij.codeInspection.lang.InspectionExtensionsFactory
import com.intellij.codeInspection.lang.RefManagerExtension
import com.intellij.codeInspection.reference.RefElement
import com.intellij.codeInspection.reference.RefEntity
import com.intellij.codeInspection.reference.RefManager
import com.intellij.codeInspection.reference.RefVisitor
import com.intellij.lang.Language
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jdom.Element
import shop.itbug.fluttercheckversionx.tools.MyInspectionContext
import java.lang.StringBuilder

class MyInspectionExtension: InspectionExtensionsFactory() {

    override fun createGlobalInspectionContextExtension(): GlobalInspectionContextExtension<MyInspectionContext> {
        println("MyInspectionExtension# MyInspectionExtension")
        return MyInspectionContext()
    }

    override fun createRefManagerExtension(refManager: RefManager?): RefManagerExtension<*> {
        println("createRefManagerExtension###")
        return object : RefManagerExtension<String> {
            override fun getID(): Key<String> {
                return Key.create("createRefManagerExtension")
            }

            @Deprecated("Deprecated in Java", ReplaceWith("Language.ANY", "com.intellij.lang.Language"))
            override fun getLanguage(): Language {
                return Language.ANY
            }

            override fun iterate(visitor: RefVisitor) {
            }

            override fun cleanup() {
            }

            override fun removeReference(refElement: RefElement) {
            }

            override fun createRefElement(psiElement: PsiElement): RefElement? {
                return null
            }

            override fun getReference(type: String?, fqName: String?): RefEntity? {
                return null
            }

            override fun getType(entity: RefEntity): String? {
                return null
            }

            override fun getRefinedElement(ref: RefEntity): RefEntity {
                return ref
            }

            override fun visitElement(element: PsiElement) {

            }

            override fun getGroupName(entity: RefEntity): String? {
                return null
            }

            override fun belongsToScope(psiElement: PsiElement): Boolean {
                return false
            }

            override fun export(refEntity: RefEntity, element: Element) {
            }

            override fun onEntityInitialized(refEntity: RefElement, psiElement: PsiElement) {

            }

        }
    }

    override fun createHTMLComposerExtension(composer: HTMLComposer?): HTMLComposerExtension<*> {
        println("createHTMLComposerExtension__==")
        return object : HTMLComposerExtension<String> {
            override fun getID(): Key<String> {
                return Key.create("createHTMLComposerExtension")
            }

            override fun getLanguage(): Language {
                return Language.ANY
            }

            override fun appendShortName(entity: RefEntity?, buf: StringBuilder) {
            }

            override fun appendLocation(entity: RefEntity?, buf: StringBuilder) {
            }

            override fun getQualifiedName(entity: RefEntity?): String? {
                return null
            }

            override fun appendReferencePresentation(
                entity: RefEntity?,
                buf: StringBuilder,
                isPackageIncluded: Boolean
            ) {
            }

        }
    }

    override fun isToCheckMember(element: PsiElement, id: String): Boolean {
        return false
    }

    override fun getSuppressedInspectionIdsIn(element: PsiElement): String? {
        return null
    }
}