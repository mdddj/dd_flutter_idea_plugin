package shop.itbug.fluttercheckversionx.request

import cn.hutool.http.HttpUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.components.BorderLayoutPanel
import shop.itbug.fluttercheckversionx.util.toast
import shop.itbug.fluttercheckversionx.util.toastWithError
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JToolBar
import javax.swing.SwingConstants


class RequestPanelUi(private val toolWindow: ToolWindow,private val project: Project): BorderLayoutPanel() {

    private val apiInput = JBTextField()
    private val generateButton = JButton("Send")

    init {
        addToTop(createToolbar())
        generateButton.addActionListener { getApiJson() }
    }

    ///创建顶部操作区域
    private fun createToolbar(): JComponent {
        val toolbar = JToolBar()
        toolbar.add(apiInput)
        toolbar.add(generateButton)
        toolbar.isFloatable = false
        toolbar.orientation = SwingConstants.HORIZONTAL
        return toolbar
    }

    private fun getApiJson()   {
        val api = apiInput.text
        if(validUrl(api)){
            try{
               val response = HttpUtil.get(api)
                println(response)
            }catch (e:Exception){
                project.toast(e.localizedMessage)
            }
        }else {
            project.toastWithError("Unable to access URL")
        }
    }


    private fun validUrl(urlString: String) : Boolean {
        try {
            val url = URL(urlString)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setRequestMethod("HEAD")
            val responseCode: Int = connection.getResponseCode()
            return if (responseCode == HttpURLConnection.HTTP_OK) {
                println("URL is accessible")
                true
            } else {
                println("URL is not accessible")
                false
            }
        } catch (e: Exception) {
            println("Invalid URL")
            return false
        }
    }

}