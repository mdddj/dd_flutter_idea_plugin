package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import shop.itbug.fluttercheckversionx.document.getDartElementType
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * 处理inlay代码点击 (dart type)
 */
class DartTypeClickActionHandle : InlayActionHandler {

    override fun handleClick(editor: Editor, payload: InlayActionPayload) {
        when (payload) {
            is PsiPointerInlayActionPayload -> {
                payload.pointer.element?.let {
                    findUseAge(it, editor)
                }
            }

            is StringInlayActionPayload -> {}
//            is SymbolPointerInlayActionPayload -> {}
            is SymbolPointerInlayActionPayload -> {}
        }
    }


    //查找类型的定义
    private fun findUseAge(element: PsiElement, editor: Editor) {

        val typeText = element.getDartElementType()
        val project = element.project
        if (typeText != null) {
            val findType = MyDartPsiElementUtil.searchClassByText(element.project, typeText)
            if (findType != null) {
                findType.navigate(true)
            } else {
//                WriteCommandAction.runWriteCommandAction(project) {
//                    editor.document.insertString(element.textRange.startOffset, " $typeText ")
//                }
            }
        }
    }
}