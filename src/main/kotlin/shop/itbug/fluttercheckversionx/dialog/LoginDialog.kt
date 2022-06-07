package shop.itbug.fluttercheckversionx.dialog

import WidgetTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.compose.theme.typography
import java.awt.Dimension
import javax.swing.JComponent

class LoginDialog(project: Project) : DialogWrapper(project) {

    init {
        title = "典典账号登录"
        init()
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(520, 360)
    }

    ///执行登录的方法
    fun login(username: String, password: String) {

    }

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setBounds(0, 0, 520, 360)
            setContent {
                WidgetTheme(darkTheme = true) {
                    var username by remember { mutableStateOf(TextFieldValue("")) }
                    var password by remember { mutableStateOf(TextFieldValue("")) }
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(30.dp)
                        ) {
                            Text(text = "登录", style = typography.h3)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(22.dp)
                            ) {
                                Row {
                                    TextField(
                                        value = username, onValueChange = { v: TextFieldValue -> username = v },
                                        placeholder = { Text("请输入典典账号") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row {
                                    TextField(
                                        value = password, onValueChange = { v: TextFieldValue -> password = v },
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            keyboardType = KeyboardType.Password
                                        ),
                                        placeholder = { Text("请输入密码") }, modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(onClick = {
                                    login(username.text, password = password.text)
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("立即 登录")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}