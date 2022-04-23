package form;

import com.intellij.openapi.project.Project;
import form.sub.CustomListRender;
import form.sub.JsonValueRender;
import socket.ProjectSocketService;
import util.ScreenUtil;

import javax.swing.*;
import java.awt.*;


///展示详情页面的小组件
public class RequestDetailForm extends JDialog {

    private ProjectSocketService.SocketResponseModel model;
    private final JPanel detailPanel;


    /**
     * 构造函数
     * <p>
     * <p>
     * 对窗体的一系列组件进行初始化
     *
     * @param model 模型
     */
    public RequestDetailForm(ProjectSocketService.SocketResponseModel model, Project project) {


        setTitle("请求详情");

        this.model = model;


        Object body = model.getBody();


        detailPanel = new JPanel(new GridLayout(1, 2));


        Dimension baseScreenSize = ScreenUtil.Companion.getBaseScreenSize();


        detailPanel.setSize(baseScreenSize);
        detailPanel.setMinimumSize(baseScreenSize);
        detailPanel.setPreferredSize(baseScreenSize);
        detailPanel.setMaximumSize(baseScreenSize);


        Dimension dimension = new Dimension(baseScreenSize.width - (baseScreenSize.width - 200), baseScreenSize.height);


        detailPanel.add(new CustomListRender(model, project));
        detailPanel.add(new JsonValueRender(
                "返回数据",
                body,
                project,
                null
        ));


        setMaximumSize(baseScreenSize);
        setSize(baseScreenSize);

        setContentPane(detailPanel);


    }


    public JDialog getContent() {
        return this;
    }


}

