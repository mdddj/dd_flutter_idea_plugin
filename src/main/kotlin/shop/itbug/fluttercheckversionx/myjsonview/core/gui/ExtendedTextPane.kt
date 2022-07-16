package shop.itbug.fluttercheckversionx.myjsonview.core.gui

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import javax.swing.text.BadLocationException

class ExtendedTextPane : RSyntaxTextArea() {

    @Throws(BadLocationException::class)
    fun setCaretLineNumber(line: Int) {
        val rootElement = document.defaultRootElement
        if (line < 1 || line > rootElement.elementCount) {
            throw BadLocationException("无效的行", line)
        }
        val offset = rootElement.getElement(line - 1).startOffset
        caretPosition = offset
    }


    //    获取总行数
    override fun getLineCount(): Int {
        return document.defaultRootElement.elementCount
    }
}