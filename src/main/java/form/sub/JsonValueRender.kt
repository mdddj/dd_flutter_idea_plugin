package form.sub

import com.google.gson.GsonBuilder
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */
class JsonValueRender(title: String, private val jsonObject: Any, var project: Project, private val dim: Dimension?): JPanel() {
    init {


        layout = BorderLayout(0,12)

        val jLabel = JLabel(title)

        add(jLabel,BorderLayout.PAGE_START)
        add(createJsonEditer(),BorderLayout.CENTER)

    }

    /**
     * 创建一个json viewer
     */
    private fun createJsonEditer(): JComponent {

        val gsonBuilder = GsonBuilder()
        gsonBuilder.setPrettyPrinting()
        val create = gsonBuilder.create()


        var obj = jsonObject
        var isJson = true
        if(jsonObject is String){
            try {
                obj = create.fromJson(jsonObject,Map::class.java)
            }catch (e: Exception){
                ///说明返回的数据不是json类型的数据
                isJson = false
            }
        }

        return if(isJson){
            val jsonString = create.toJson(obj)
            val languageTextField = LanguageTextField(JsonLanguage.INSTANCE, project, jsonString, false)

            val jbScrollPane = JBScrollPane(languageTextField)
            if(dim!=null){
                jbScrollPane.minimumSize = dim
            }
            jbScrollPane
        }else{
            JTextArea(jsonObject.toString())
        }

    }
}