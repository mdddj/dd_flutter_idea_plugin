package shop.itbug.fluttercheckversionx.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import shop.itbug.fluttercheckversionx.services.*
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel

class LoginDialog(project: Project) : DialogWrapper(project) {

    private var stateModel : MyUserState? = null
    init {
        title = "典典账号登录"
        init()
        val state = PluginStateService.getInstance().state
        stateModel = state
        println(stateModel)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(520, 360)
    }

    /**
     * 执行登录的方法
     */
    private  fun login(username: String, password: String) {
        println("进来了...${username} $password")
        stateModel?.username = username
        runBlocking {
            launch {
                val result = ServiceCreateWithMe.create<ItbugService>().login(LoginParam(
                    username,
                    password
                ))
                if(result.state.ok()){
                    println(result.data)
                }else{
                    println("登录失败:${result.message}")
                }
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        return JLabel("登录")
    }
}