package shop.itbug.fluttercheckversionx.widget

import com.intellij.ui.components.fields.ExtendableTextField
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory

class MySimpleTextField(placeholder: String = "请输入内容", onSubmit: (value: String) -> Unit) : ExtendableTextField() {
    init {
        border = BorderFactory.createEmptyBorder()
        emptyText.text = placeholder
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                super.keyPressed(e)
                if (e != null && text.isNotEmpty()) {
                    if ((e.keyCode == 10) && (e.keyChar == '\n')) {
                        onSubmit.invoke(text)
                    }
                }
            }
        })
    }
}