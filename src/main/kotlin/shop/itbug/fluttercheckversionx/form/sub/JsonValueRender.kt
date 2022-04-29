package shop.itbug.fluttercheckversionx.form.sub

import com.google.gson.GsonBuilder
import com.intellij.json.JsonLanguage
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * json viewer
 *
 * 展示一个json的组件
 *
 *
 */
class JsonValueRender(private val jsonObject: Any, var project: Project): JPanel() {


    private lateinit var jsonView: LanguageTextField

    init {

        layout = BorderLayout(0,12)
        border = BorderFactory.createEmptyBorder()


        //创建展示json区域
        createJsonEditer()

        add(jsonView,BorderLayout.CENTER)

    }


    /**
     * 外部调用,改变json内容
     */
    fun changeValue(json: Any?){
        if(json!=null){
            val changeJson = changeJson(json)
            jsonView.text = changeJson
        }
    }

    /**
     * 改变显示内容
     *
     * 返回要显示的json string
     */
   private fun changeJson(json:Any): String{

        val gsonBuilder = GsonBuilder()
        gsonBuilder.setPrettyPrinting()
        val create = gsonBuilder.create()


        var obj = json
        var isJson = true
        if(json is String){
            try {
                obj = create.fromJson(json,Map::class.java)
            }catch (e: Exception){
                ///说明返回的数据不是json类型的数据
                isJson = false
            }
        }
        return if(isJson) {
            create.toJson(obj)
        }else{
            json.toString()
        }
    }

    /**
     * 创建一个json viewer
     */
    private fun createJsonEditer() {

        jsonView = LanguageTextField(JsonLanguage.INSTANCE, project, changeJson(jsonObject), false)
        jsonView.border = BorderFactory.createEmptyBorder()


    }
}