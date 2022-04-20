package inlay.dartfile

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.lang.dart.DartElementType
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService
import com.jetbrains.lang.dart.ide.documentation.DartDocUtil
import com.jetbrains.lang.dart.psi.DartType
import com.jetbrains.lang.dart.psi.DartTypedFunctionType
import com.jetbrains.lang.dart.psi.impl.DartArgumentListImpl
import com.jetbrains.lang.dart.psi.impl.DartArgumentsImpl
import com.jetbrains.lang.dart.psi.impl.DartCallExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartClassReferenceImpl
import com.jetbrains.lang.dart.psi.impl.DartComponentNameImpl
import com.jetbrains.lang.dart.psi.impl.DartListLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartLiteralExpressionImpl
import com.jetbrains.lang.dart.psi.impl.DartReferenceExpressionImpl
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
                        val filterIsInstance =
                            filterIsInstanceWithName.children.filterIsInstance<DartComponentNameImpl>()[0]
                        val analysisGethover = DartAnalysisServerService.getInstance(file.project)
                            .analysis_getHover(file.virtualFile, filterIsInstance.textOffset)
                        if (analysisGethover.isNotEmpty()) {
                            val staticType = analysisGethover[0].staticType
                            sink.addInlineElement(
                                filterIsInstanceWithName.endOffset, false,
                                hintsInlayPresentationFactory.simpleText(staticType, "类型:$staticType"), false
                            )
                        }
                    }
                }


//                方法调用提示文本, 太影响格式了,后面优化

//                if (element is DartCallExpressionImpl) {
//                    val args =
//                        element.children.filterIsInstance<DartArgumentsImpl>()[0].children.filterIsInstance<DartArgumentListImpl>()
//                    for (ai in args) {
//                        val argsList = ai.children
//                        for (al in argsList) {
//                            val analysisGethover = DartAnalysisServerService.getInstance(file.project)
//                                .analysis_getHover(file.virtualFile, al.textOffset)
//                            if (analysisGethover.isNotEmpty()) {
//                                val type = analysisGethover[0].staticType
//                                sink.addInlineElement(
//                                    al.startOffset, false,
//                                    hintsInlayPresentationFactory.simpleText("$type:", "参数类型:$type"), false
//                                )
//                            }
//                        }
//                    }
//
//                }
                return true
            }

        }
    }

    fun getExpressionValueType() {

    }


    fun getElementType(element: DartVarDeclarationListImpl) {


        val filterIsInstance = element.varInit?.children?.filterIsInstance<DartLiteralExpressionImpl>()



        if (filterIsInstance != null && filterIsInstance.isNotEmpty()) {

            var obj = filterIsInstance[0]
            println(
                "-->${obj.context}  ${obj.get()}  ${obj.canonicalText} ${obj.acquire} ${obj.opaque}" +
                        "" +
                        "${obj.plain}  ${obj.reference?.canonicalText} "
            )
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