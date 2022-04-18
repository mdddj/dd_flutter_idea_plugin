package inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.lang.dart.DartElementType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.psi.DartType
import com.jetbrains.lang.dart.psi.DartTypedFunctionType
import com.jetbrains.lang.dart.psi.impl.DartClassReferenceImpl
import com.jetbrains.lang.dart.psi.impl.DartListLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartVarAccessDeclarationImpl
import com.jetbrains.lang.dart.psi.impl.DartVarDeclarationListImpl
import inlay.HintsInlayPresentationFactory
import io.flutter.dart.DartPsiUtil
import javax.swing.JComponent
import javax.swing.JLabel

class DartTypeInlayHintsProvider : InlayHintsProvider<DartTypeInlayHintsProvider.Setting> {


    data class Setting(private val enable: Boolean)

    override val key: SettingsKey<Setting>
        get() = SettingsKey("dart.type.inlay.hints.provider")
    override val name: String
        get() = "dart.type.inlay.hints.provider"
    override val previewText: String?
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
    ): InlayHintsCollector? {
        return object : FactoryInlayHintsCollector(editor) {

            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                val hintsInlayPresentationFactory = HintsInlayPresentationFactory(factory)
                if (element is DartVarDeclarationListImpl) {
                    val nameComm = element.children.filterIsInstance<DartVarAccessDeclarationImpl>()
                    if (nameComm.isNotEmpty()) {
                        val filterIsInstanceWithName = nameComm[0]
                            val varInit = element.varInit
                            val expression = varInit?.expression?.tokenType?.toString()
                            if (expression != null) {
                                val type = expression.split("_")[0].lowercase()
                                sink.addInlineElement(
                                    filterIsInstanceWithName.endOffset, false,
                                    hintsInlayPresentationFactory.simpleText(type, "类型:$type"), false
                                )
                            }
                    }
                }
                return true
            }

        }
    }

    fun getExpressionValueType(){

    }


    fun getElementType(element: DartVarDeclarationListImpl) {


        val filterIsInstance = element.varInit?.children?.filterIsInstance<DartLiteralExpressionImpl>()



        if(filterIsInstance!=null && filterIsInstance.isNotEmpty()){

            var obj = filterIsInstance[0]
            println("-->${obj.context}  ${obj.get()}  ${obj.canonicalText} ${obj.acquire} ${obj.opaque}" +
                    "" +
                    "${obj.plain}  ${obj.reference?.canonicalText} ")
        }

//        println("1:$acquire 2:$opaque 3:$plain 4:$userDataEmpty 5:$name1 6:$node 7:$tokenType 8:$firstChild 9:${filterIsInstance?.get(0)?.canonicalText}")
    }

    override fun createConfigurable(settings: Setting): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JLabel()
            }

        }
    }

    override val group: InlayGroup
        get() = InlayGroup.TYPES_GROUP
}