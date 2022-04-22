package form;

import com.google.gson.Gson;
import com.intellij.ui.components.JBScrollPane;
import form.model.KeyValueObj;
import form.sub.SokcetListItemLayout;
import socket.ProjectSocketService;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Map;



///展示详情页面的小组件
public class RequestDetailForm {

    private ProjectSocketService.SocketResponseModel model;
    private JPanel detailPanel;
    private JList detailList;

    private ArrayList props = new ArrayList<KeyValueObj>();


    /**
     * 构造函数
     *
     *
     * 对窗体的一系列组件进行初始化
     *
     * @param model 模型
     */
    public RequestDetailForm(ProjectSocketService.SocketResponseModel model){
        this.model = model;

        initSetValue();

        detailList.setCellRenderer(new MyListCellLayoutRender());
        detailList.setListData(props.toArray());



    }


    private void initSetValue(){
        Gson gson = new Gson();
        String s = gson.toJson(model);
        Map<String,Object> map = gson.fromJson(s, Map.class);
        map.forEach((k,v)-> props.add(new KeyValueObj(k,v)));
    }


    public JComponent getContent(){
        return new JBScrollPane(this.detailPanel);
    }


}



class MyListCellLayoutRender implements   ListCellRenderer<KeyValueObj>{


    @Override
    public Component getListCellRendererComponent(JList<? extends KeyValueObj> list, KeyValueObj value, int index, boolean isSelected, boolean cellHasFocus) {
        return new SokcetListItemLayout(value).getContent();
    }
}
