package shop.itbug.fluttercheckversionx.form.test

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.BoxLayout
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class FormTest : JPanel(){


    init {
        layout = GridLayout(1,2)

        size = Dimension(400,400)
        add(LeftCom())
        add(RightCom())

    }


    class LeftCom: JPanel(){
        init {


            layout = BoxLayout(this,BoxLayout.PAGE_AXIS)

            add(
                JLabel("11")
            )
            add(
                JLabel("22")
            )
            add(
                JLabel("33")
            )
            add(
                JLabel("44")
            )
        }
    }

     class RightCom :JPanel(){

        init {
            layout = BorderLayout()
            add(JLabel("返回数据"),BorderLayout.PAGE_START)
            add(JTextArea(),BorderLayout.CENTER)
        }

    }




}

fun main(){
    val dialog = JDialog()
    dialog.add(FormTest())
    dialog.isVisible =true
    dialog.pack()

}