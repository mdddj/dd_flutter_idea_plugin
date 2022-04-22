package form;

import cn.hutool.core.lang.Console;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import form.model.KeyValueObj;
import form.sub.SokcetListItemLayout;
import socket.ProjectSocketService;
import util.ScreenUtil;

import javax.swing.*;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;


///展示详情页面的小组件
public class RequestDetailForm {

    private ProjectSocketService.SocketResponseModel model;
    private final JPanel detailPanel;
    private JBList<Object> detailList;

    private JTextArea bodyShowText;

    private final ArrayList<KeyValueObj> props = new ArrayList<>();


    /**
     * 构造函数
     * <p>
     * <p>
     * 对窗体的一系列组件进行初始化
     *
     * @param model 模型
     */
    public RequestDetailForm(ProjectSocketService.SocketResponseModel  model) {


        if (model != null) {

            this.model = model;

            initSetValue();

            createUIComponents();


            Object body = model.getBody();

            Console.log("数据类型是:{}", body.getClass());
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            Gson gson = gsonBuilder.create();

            if (body instanceof String) {
                bodyShowText.setText(body.toString());
            } else {

                ///尝试转成json
                try {
                    String s = gson.toJson(body);
                    bodyShowText.setText(s);
                } catch (Exception e) {
                    //转换失败
                    bodyShowText.setText(body.toString());
                    Console.log("解析json数据失败:{}", e.fillInStackTrace());
                }
            }

        }




        detailPanel = new JPanel(new GridLayout(1,2));


        Dimension baseScreenSize = ScreenUtil.Companion.getBaseScreenSize();

        Console.log(baseScreenSize);


        detailPanel.setSize(baseScreenSize);
        detailPanel.setMinimumSize(baseScreenSize);
        detailPanel.setMaximumSize(baseScreenSize);


        Dimension dimension = new Dimension(baseScreenSize.width - (baseScreenSize.width - 200), baseScreenSize.height);

        bodyShowText.setSize(dimension);
        bodyShowText.setMaximumSize(dimension);
        bodyShowText.setMaximumSize(dimension);
        bodyShowText.setBackground(JBColor.background());
        bodyShowText.setForeground(JBColor.foreground());
        bodyShowText.setOpaque(true);


        Highlighter highlighter = bodyShowText.getHighlighter();


        detailPanel.add(detailList);
        detailPanel.add(new JScrollPane(bodyShowText));



    }


    private void initSetValue() {
        Gson gson = new Gson();
        String s = gson.toJson(model);
        Map map = gson.fromJson(s, Map.class);
        map.forEach((k, v) -> props.add(new KeyValueObj(k.toString(), v)));


    }


    public JPanel getContent() {
        return this.detailPanel;
    }


    private void createUIComponents() {


        detailList = new JBList<>();
        bodyShowText = new JTextArea();
        detailList.setListData(props.toArray());

    }
}


class MyListCellLayoutRender implements ListCellRenderer<Object> {


    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JComponent content = new SokcetListItemLayout((KeyValueObj) value).getContent();
        content.setVisible(true);
        return content;
    }
}
