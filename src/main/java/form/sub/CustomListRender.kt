package form.sub

import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.VerticalBox
import socket.ProjectSocketService
import java.awt.Color
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

///jlist自定义渲染
class CustomListRender(private val model: ProjectSocketService.SocketResponseModel,val project: Project) : JPanel(){


    init {
        initComponentUi()
    }

    private fun initComponentUi(){


        val dimension = Dimension()
        dimension.height = 100

        layout = BoxLayout(this,BoxLayout.PAGE_AXIS)


        add(StringValueRender("Url",model.url))
        add(Box.createVerticalStrut(6))
        add(StringValueRender("Methed",model.methed))
        add(Box.createVerticalStrut(6))
        add(StringValueRender("Status Code",model.statusCode.toString()))
        if(model.data!=null){
            add(Box.createVerticalStrut(6))
            add(JsonValueRender("参数",model.data,project,dimension))
        }
        add(Box.createVerticalStrut(6))
        add(JsonValueRender("请求头",model.headers,project, dimension))
        add(Box.createVerticalStrut(6))
        add(JsonValueRender("返回头",model.responseHeaders,project,dimension))
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
                 valueLabel.foreground = Color.GREEN
             }else if(value == "500"){
                 valueLabel.foreground = Color.RED
                 valueLabel.text = "500  (服务器错误)"
             }
             add(valueLabel)

         }

     }


}