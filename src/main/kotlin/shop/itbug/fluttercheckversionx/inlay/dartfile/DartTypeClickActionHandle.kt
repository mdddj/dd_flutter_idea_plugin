package shop.itbug.fluttercheckversionx.inlay.dartfile

import com.intellij.codeInsight.hints.declarative.InlayActionHandler
import com.intellij.codeInsight.hints.declarative.InlayActionPayload
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import shop.itbug.fluttercheckversionx.document.getDartElementType
import shop.itbug.fluttercheckversionx.util.MyDartPsiElementUtil

/**
 * 处理inlay代码点击 (dart type)
 */
class DartTypeClickActionHandle : InlayActionHandler {


    //    override fun handleClick(e: EditorMouseEvent, payload: InlayActionPayload) {
//        when (payload) {
//            is PsiPointerInlayActionPayload -> {
//                payload.pointer.element?.let {
//                    findUseAge(it)
//                }
//            }
//
//            else -> {}
//        }
//    }
    override fun handleClick(editor: Editor, payload: InlayActionPayload) {
        when (payload) {
            is PsiPointerInlayActionPayload -> {
                payload.pointer.element?.let {
                    findUseAge(it)
                }
            }

            else -> {}
        }
    }

    //查找类型的定义
    private fun findUseAge(element: PsiElement) {
        val typeText = element.getDartElementType()
        if (typeText != null) {
            val findType = MyDartPsiElementUtil.searchClassByText(element.project, typeText)
            findType?.navigate(true)
        }
    }
}