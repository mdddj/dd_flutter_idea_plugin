package form.socket;

import cn.hutool.core.lang.Console;
import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;

// 监听http请求的窗口
public class SocketRequestForm {
    private JPanel myJanel;
    private JTable table1;
    private JLabel tips;


    public SocketRequestForm(ToolWindow toolWindow){
        Console.log("窗口构建了");
    }


    public JPanel getContent(){
        return  this.myJanel;
    }

    private void createUIComponents() {
    }
}
