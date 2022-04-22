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

    public SokcetListItemLayout(KeyValueObj model){

        contentJPanel = new JPanel();

        keyLable = new JLabel();
        valueLabel = new JLabel();

        keyLable.setText(model.getKey());
        valueLabel.setText(model.getValue().toString());

        contentJPanel.add(keyLable);
        contentJPanel.add(valueLabel);
    }

    public JComponent getContent(){
        return contentJPanel;
    }
}
