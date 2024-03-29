package shop.itbug.fluttercheckversionx.form.sub

import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import shop.itbug.fluttercheckversionx.socket.ProjectSocketService
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

///jlist自定义渲染
class CustomListRender(private val model: ProjectSocketService.SocketResponseModel, val project: Project) : JPanel(){


    init {
        initComponentUi()
    }

    private fun initComponentUi(){


        val dimension = Dimension()
        dimension.height = 100

        layout = BoxLayout(this,BoxLayout.PAGE_AXIS)


        add(StringValueRender("Url",model.url?:""))
        add(Box.createVerticalStrut(6))
        add(StringValueRender("Methed",model.method?:""))
        add(Box.createVerticalStrut(6))
        add(StringValueRender("Status Code",model.statusCode.toString()))
        if(model.data!=null){
            add(Box.createVerticalStrut(6))
            add(JsonValueRender(project))
        }
        add(Box.createVerticalStrut(6))
        add(JsonValueRender(project))
        add(Box.createVerticalStrut(6))
        add(JsonValueRender(project))
    }


    /**
     * 渲染普通类型的值
     */
    class StringValueRender(title: String, value: String): JPanel() {

         init {


             layout = BoxLayout(this,BoxLayout.PAGE_AXIS)

             val jLabel = JLabel(title)
             add(jLabel)
             add(Box.createVerticalStrut(6))

             val valueLabel = JLabel(value)
             if(value == "200"){
                 valueLabel.foreground = UIUtil.getLabelInfoForeground()
             }else if(value == "500"){
                 valueLabel.foreground = UIUtil.getErrorForeground()
                 valueLabel.text = "500  (服务器错误)"
             }
             add(valueLabel)

         }

     }


}