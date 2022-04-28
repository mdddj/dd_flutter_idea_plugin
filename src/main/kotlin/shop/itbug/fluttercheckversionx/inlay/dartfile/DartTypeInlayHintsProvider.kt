package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.impl.*
import shop.itbug.fluttercheckversionx.inlay.HintsInlayPresentationFactory
import shop.itbug.fluttercheckversionx.inlay.json.DefaulImmediateConfigurable

class DartTypeInlayHintsProvider : InlayHintsProvider<DartTypeInlayHintsProvider.Setting> {


    data class Setting(private val enable: Boolean)

    override val key: SettingsKey<Setting>
        get() = SettingsKey("dart.type.inlay.hints.provider")
    override val name: String
        get() = "dart.type.inlay.hints.provider"
    override val previewText: String
        get() = """
            预览
        """.trimIndent()

    override fun createSettings(): Setting {
        return Setting(true)
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Setting,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return object : FactoryInlayHintsCollector(editor) {

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val hintsInlayPresentationFactory = HintsInlayPresentationFactory(factory)
                if (element is DartVarDeclarationListImpl) {
                    val nameComm = element.children.filterIsInstance<DartVarAccessDeclarationImpl>()
                    if (nameComm.isNotEmpty()) {
                        val filterIsInstanceWithName = nameComm[0]
                        val filterIsInstance =
                            filterIsInstanceWithName.children.filterIsInstance<DartComponentNameImpl>()[0]
                       filterIsInstance.parent.nextSibling ?: return true
                        val analysisGethover = DartAnalysisServerService.getInstance(file.project)
                            .analysis_getHover(file.virtualFile, filterIsInstance.textOffset)
                        if (analysisGethover.isNotEmpty()) {
                            val staticType = analysisGethover[0].staticType
                            if(staticType!=null){
                                sink.addInlineElement(
                                    filterIsInstanceWithName.endOffset, false,
                                    hintsInlayPresentationFactory.simpleText(staticType, "类型:$staticType"), false
                                )
                            }
                        }
                    }
                }

                /// Doc 类型的超级注解
                if (element is DartReferenceExpressionImpl) {
                    val resolve = element.reference?.resolve()
                    if(resolve!=null){
                        val firstChild = resolve.parent.firstChild
                        if(firstChild is DartMetadataImpl){
                            val doc = firstChild.firstChild.nextSibling
                            if(doc!=null && doc.text == "Doc"){
                                val children = doc.nextSibling.children.filterIsInstance<DartArgumentListImpl>()
                                if(children.isNotEmpty()){
                                    val args = children[0].children.filterIsInstance<DartNamedArgumentImpl>() /// 参数列表
                                    for(arg in args){
                                        if(arg.firstChild.text=="message"){
                                            val docMessage = arg.lastChild.text.replace("\"","")
                                            if(element.parent.nextSibling!=null){
                                                sink.addInlineElement(element.parent.nextSibling.endOffset,
                                                    false,
                                                    hintsInlayPresentationFactory.simpleText(docMessage,""),
                                                    false
                                                )
                                            }
                                        }
                                    }
                                }

                            }
                        }
                    }

                }
                return true
            }

        }
    }


    override fun createConfigurable(settings: Setting): ImmediateConfigurable {
        return DefaulImmediateConfigurable()
    }

    override val group: InlayGroup
        get() = InlayGroup.TYPES_GROUP
}