package shop.itbug.fluttercheckversionx.widget

import com.intellij.ui.components.fields.ExtendableTextField
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory

typealias MySimpleTextFieldSubmit = (value: String) -> Unit

class MySimpleTextField(
    placeholder: String = "Please enter content",
    initValue: String? = null,
    onSubmit: MySimpleTextFieldSubmit,
) :
    ExtendableTextField() {

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

        if (initValue != null) {
            text = initValue
        }
    }

    override fun getPreferredSize(): Dimension? {
        return Dimension(300, 40)
    }
}