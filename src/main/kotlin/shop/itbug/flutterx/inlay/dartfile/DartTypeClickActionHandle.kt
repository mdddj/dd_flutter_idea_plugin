package shop.itbug.flutterx.inlay.dartfile

import com.intellij.codeInsight.hints.declarative.InlayActionHandler
import com.intellij.codeInsight.hints.declarative.InlayActionPayload
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import shop.itbug.flutterx.document.getDartElementType
import shop.itbug.flutterx.util.MyDartPsiElementUtil
import com.intellij.util.concurrency.AppExecutorUtil

/**
 * 处理inlay代码点击 (dart type)
 */
class DartTypeClickActionHandle : InlayActionHandler {

    override fun handleClick(e: EditorMouseEvent, payload: InlayActionPayload) {
        when (payload) {
            is PsiPointerInlayActionPayload -> {
                findUseAge(payload.pointer)
            }

            else -> {}
        }
    }
//    override fun handleClick(editor: Editor, payload: InlayActionPayload) {
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

    // 查找类型的定义
    private fun findUseAge(pointer: SmartPsiElementPointer<out PsiElement>) {
        val project = pointer.project
        ReadAction
            .nonBlocking<DartTypeNavigationTarget?> {
                if (project.isDisposed) return@nonBlocking null
                val element = pointer.element ?: return@nonBlocking null
                if (!element.isValid) return@nonBlocking null
                val typeText = element.getDartElementType() ?: return@nonBlocking null
                val findType = MyDartPsiElementUtil.searchClassByText(project, typeText) ?: return@nonBlocking null
                val virtualFile = findType.containingFile?.virtualFile ?: return@nonBlocking null
                DartTypeNavigationTarget(project, virtualFile, findType.textOffset)
            }
            .inSmartMode(project)
            .withDocumentsCommitted(project)
            .coalesceBy(this, pointer)
            .finishOnUiThread(ModalityState.defaultModalityState()) { target ->
                if (target != null && !target.project.isDisposed && target.file.isValid) {
                    OpenFileDescriptor(target.project, target.file, target.offset).navigate(true)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private data class DartTypeNavigationTarget(
        val project: Project,
        val file: VirtualFile,
        val offset: Int
    )
}
