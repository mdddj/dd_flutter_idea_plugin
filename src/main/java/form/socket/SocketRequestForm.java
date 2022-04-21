package form.socket;

import cn.hutool.core.lang.Console;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.table.JBTable;
import services.SokcetMessageBus;
import socket.ProjectSocketService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// 监听http请求的窗口
public class SocketRequestForm {
    private JPanel myJanel;
    private JTable table1;
    private JToolBar dioToolBar;
    private JLabel refreshLable;

    private int columnLen = 4;

    DioRequestTableModel dioRequestTableModel;


    public SocketRequestForm(ToolWindow toolWindow) {
        Project defaultProject = toolWindow.getProject();
       refreshData(defaultProject);
        defaultProject.getMessageBus().connect().subscribe(SokcetMessageBus.CHANGE_ACTION_TOPIC, data -> {
            refreshData(defaultProject);
        });


        updateRowWidth();


    }

    /// 改变列宽
    private void updateRowWidth(){

    }



    /// 刷新数据
    public void refreshData(Project project){
        SwingUtilities.invokeLater(() -> {
            ProjectSocketService service = project.getService(ProjectSocketService.class);
            dioRequestTableModel = new DioRequestTableModel(4, service.getRequests());
            table1.setModel(dioRequestTableModel);
        });

    }


    public JPanel getContent() {
        return this.myJanel;
    }

    private void createUIComponents() {
        table1 = new JBTable(new DioRequestTableModel(columnLen,new ArrayList<>()));
    }
}


class DioRequestTableModel extends AbstractTableModel {

    private int column; // 列
    private List<ProjectSocketService.SocketResponseModel> datas; // 数据列表


    public List<ProjectSocketService.SocketResponseModel> getDatas(){
        return datas;
    }

    public DioRequestTableModel(int column, List<ProjectSocketService.SocketResponseModel> datas) {
        this.datas = datas;
        this.column = column;
    }


    @Override
    public int getRowCount() {
        return datas.size();
    }

    @Override
    public int getColumnCount() {
        return this.column;
    }


    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Url";
            case 1:
                return "Methed";
            case 2:
                return "Status";
            case 3:
                return "Timestamp";
        }
        return super.getColumnName(column);
    }


    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ProjectSocketService.SocketResponseModel data = datas.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return data.getUrl();
            case 1:
                return data.getMethed();
            case 2:
                return data.getStatusCode();
            case 3:
                return data.getData();
            default:
                break;
        }
        return "1";
    }
}