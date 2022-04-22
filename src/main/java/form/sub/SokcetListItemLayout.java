package form.sub;

import form.model.KeyValueObj;

import javax.swing.*;

/**
 * 这里是展示请求详情item 项目的布局
 */
public class SokcetListItemLayout {


    private JPanel contentJPanel;
    private JLabel keyLable;
    private JLabel valueLabel;

    private KeyValueObj model;

    public SokcetListItemLayout(KeyValueObj model){
        this.model = model;


        keyLable.setText(model.getKey());
        if(model.getValue() instanceof String){
            valueLabel.setText(model.getValue().toString());
        }else{
            valueLabel.setVisible(false);//设置不可见
        }
    }





    public JComponent getContent(){
        return contentJPanel;
    }
}
